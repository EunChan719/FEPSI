import torch
import torch.nn as nn
from torch.utils.data import DataLoader, Dataset
from transformers import AutoModel, AutoTokenizer, VideoMAEModel, VideoMAEImageProcessor
from torch.optim import AdamW
from datasets import load_dataset
from tqdm import tqdm
import numpy as np

# 1. 장치 설정 및 혼합 정밀도(AMP) 사용 여부 결정
if torch.cuda.is_available():
    device = torch.device("cuda")
    use_amp = True
elif torch.backends.mps.is_available():
    device = torch.device("mps")
    use_amp = False  # MPS는 현재 완전한 AMP 지원이 제한적일 수 있음
else:
    device = torch.device("cpu")
    use_amp = False

print(f"🚀 {device} 장치에서 학습을 진행합니다. (AMP: {use_amp})")

# 2. 데이터셋 클래스 (전처리를 내부에서 수행하여 속도 향상)
class LambdaDataset(Dataset):
    def __init__(self, hf_dataset, tokenizer, feature_extractor):
        self.data = hf_dataset
        self.tokenizer = tokenizer
        self.feature_extractor = feature_extractor

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        item = self.data[idx]
        
        # 텍스트 구성
        scenes = item['ad_details'].get('Scenes', [])
        desc = " ".join([s.get('Description', '') for s in scenes[:3]])
        brand = item['ad_details'].get('Brand', 'Unknown')
        text_input = f"Brand: {brand}. Scene: {desc}"
        
        # 텍스트 인코딩
        text_encoding = self.tokenizer(
            text_input, 
            return_tensors="pt", 
            padding='max_length', 
            truncation=True, 
            max_length=128
        )
        
        # 비디오 전처리 (현재는 랜덤 텐서, 실제 환경에선 cv2 등으로 로드 필요)
        video_placeholder = np.random.randint(0, 255, (16, 224, 224, 3), dtype=np.uint8)
        video_values = self.feature_extractor(list(video_placeholder), return_tensors="pt").pixel_values
        
        return {
            "input_ids": text_encoding['input_ids'].squeeze(0),
            "attention_mask": text_encoding['attention_mask'].squeeze(0),
            "pixel_values": video_values.squeeze(0),
            "label": torch.tensor(item['recall_score'], dtype=torch.float)
        }

# 3. 모델 정의 (백본 가중치 고정)
class RecallPredictionModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.text_encoder = AutoModel.from_pretrained("bert-base-uncased")
        self.video_encoder = VideoMAEModel.from_pretrained("MCG-NJU/videomae-base")
        
        # [최적화] 가중치 고정 (Freezing)
        for param in self.text_encoder.parameters():
            param.requires_grad = False
        for param in self.video_encoder.parameters():
            param.requires_grad = False

        self.regressor = nn.Sequential(
            nn.Linear(768 + 768, 256),
            nn.ReLU(),
            nn.Linear(256, 1),
            nn.Sigmoid()
        )

    def forward(self, text_ids, text_mask, video_pixel_values):
        t_out = self.text_encoder(input_ids=text_ids, attention_mask=text_mask).last_hidden_state[:, 0, :]
        v_out = self.video_encoder(video_pixel_values).last_hidden_state.mean(dim=1)
        combined = torch.cat((t_out, v_out), dim=1)
        return self.regressor(combined).squeeze()

# 4. 학습 함수
def train_one_epoch(model, loader, optimizer, criterion, scaler, device, use_amp):
    model.train()
    total_loss = 0
    for batch in tqdm(loader, desc="Training"):
        input_ids = batch['input_ids'].to(device)
        attention_mask = batch['attention_mask'].to(device)
        pixel_values = batch['pixel_values'].to(device)
        labels = batch['label'].to(device)

        optimizer.zero_grad()
        
        
        with torch.amp.autocast(device_type=device.type, enabled=use_amp):
            outputs = model(input_ids, attention_mask, pixel_values)
            loss = criterion(outputs, labels)
        
        if use_amp:
            scaler.scale(loss).backward()
            scaler.step(optimizer)
            scaler.update()
        else:
            loss.backward()
            optimizer.step()
            
        total_loss += loss.item()
    return total_loss / len(loader)

# 5. 평가 함수
def evaluate(model, loader, criterion, device, use_amp):
    model.eval()
    total_loss = 0
    with torch.no_grad():
        for batch in tqdm(loader, desc="Evaluating"):
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            pixel_values = batch['pixel_values'].to(device)
            labels = batch['label'].to(device)
            
            with torch.amp.autocast(device_type=device.type, enabled=use_amp):
                outputs = model(input_ids, attention_mask, pixel_values)
                loss = criterion(outputs, labels)
            total_loss += loss.item()
    return total_loss / len(loader)

# 6. 메인 실행부 (멀티프로세싱 오류 방지를 위해 필수)
if __name__ == '__main__':
    # 데이터 로딩
    print("📦 데이터셋 로딩 중...")
    raw_dataset = load_dataset("behavior-in-the-wild/LAMBDA")
    tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")
    feature_extractor = VideoMAEImageProcessor.from_pretrained("MCG-NJU/videomae-base")

    train_ds = LambdaDataset(raw_dataset['train'], tokenizer, feature_extractor)
    test_ds = LambdaDataset(raw_dataset['test'], tokenizer, feature_extractor)

    # DataLoader 설정 (num_workers로 병렬 로딩 활성화)
    train_loader = DataLoader(train_ds, batch_size=8, shuffle=True, num_workers=2)
    test_loader = DataLoader(test_ds, batch_size=8, num_workers=2)

    # 모델 및 최적화 설정
    model = RecallPredictionModel().to(device)
    optimizer = AdamW(model.parameters(), lr=1e-4)
    criterion = nn.MSELoss()
    
    # 최신 버전의 GradScaler 설정
    scaler = torch.amp.GradScaler(device.type, enabled=use_amp)

    epochs = 10
    print("🔥 학습을 시작합니다.")
    for epoch in range(epochs):
        train_loss = train_one_epoch(model, train_loader, optimizer, criterion, scaler, device, use_amp)
        test_loss = evaluate(model, test_loader, criterion, device, use_amp)
        print(f"🌟 Epoch {epoch+1}/{epochs} | Train Loss: {train_loss:.4f} | Test Loss: {test_loss:.4f}")


    torch.save(model.state_dict(), "recall_model_optimized.pth")
    print("✅ 모든 프로세스가 완료되었습니다.")