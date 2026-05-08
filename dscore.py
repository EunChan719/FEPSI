import torch
import torch.nn as nn
import cv2
import numpy as np
import librosa
import os
from deepface import DeepFace
from moviepy.editor import VideoFileClip
from transformers import AutoModel, AutoTokenizer, VideoMAEModel, VideoMAEImageProcessor

# 1. AI 모델 구조 정의 (학습 시와 동일)
class RecallPredictionModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.text_encoder = AutoModel.from_pretrained("bert-base-uncased")
        self.video_encoder = VideoMAEModel.from_pretrained("MCG-NJU/videomae-base")
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

class DopamineAnalyzer:
    def __init__(self, video_path, model_path="recall_model_optimized.pth"):
        self.video_path = video_path
        self.audio_path = os.path.splitext(video_path)[0] + "_temp.mp3"
        
        # 장치 설정 (MPS/CUDA/CPU)
        self.device = torch.device("mps" if torch.backends.mps.is_available() else "cuda" if torch.cuda.is_available() else "cpu")
        
        # AI 모델 및 프로세서 초기화
        print(f"🤖 AI 모델 로딩 중... ({self.device})")
        self.model = RecallPredictionModel().to(self.device)
        if os.path.exists(model_path):
            self.model.load_state_dict(torch.load(model_path, map_location=self.device))
            print("✅ 학습된 가중치를 성공적으로 불러왔습니다.")
        else:
            print("⚠️ 가중치 파일을 찾을 수 없어 초기 상태로 분석합니다.")
        self.model.eval()

        self.tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")
        self.feature_extractor = VideoMAEImageProcessor.from_pretrained("MCG-NJU/videomae-base")

    def _extract_audio(self):
        try:
            video = VideoFileClip(self.video_path)
            if video.audio is not None:
                video.audio.write_audiofile(self.audio_path, logger=None)
                return True
            return False
        except Exception: return False

    # [AI Recall Score 계산 함수]
    def get_ai_recall_score(self):
        cap = cv2.VideoCapture(self.video_path)
        frames = []
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        if total_frames <= 0: return 0.5
        
        # 16프레임 추출
        indices = np.linspace(0, total_frames - 1, 16).astype(int)
        for idx in indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            ret, frame = cap.read()
            if ret:
                frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                frames.append(frame)
        cap.release()

        # 전처리 및 추론
        video_inputs = self.feature_extractor(list(frames), return_tensors="pt").to(self.device)
        text_inputs = self.tokenizer("A high-impact promotional video.", return_tensors="pt", padding=True, truncation=True).to(self.device)

        with torch.no_grad():
            prediction = self.model(text_inputs.input_ids, text_inputs.attention_mask, video_inputs.pixel_values)
        
       
        return prediction.item() * 100

    def get_visual_score(self):
        cap = cv2.VideoCapture(self.video_path)
        motion_scores, saturation_scores = [], []
        ret, prev_frame = cap.read()
        if not ret: return 0, 0
        prev_gray = cv2.cvtColor(prev_frame, cv2.COLOR_BGR2GRAY)
        count = 0
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret: break
            if count % 10 == 0:
                curr_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                flow = cv2.calcOpticalFlowFarneback(prev_gray, curr_gray, None, 0.5, 3, 15, 3, 5, 1.2, 0)
                mag, _ = cv2.cartToPolar(flow[..., 0], flow[..., 1])
                motion_scores.append(np.mean(mag))
                hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
                saturation_scores.append(np.mean(hsv[:, :, 1]))
                prev_gray = curr_gray
            count += 1
        cap.release()
        m_score = min(100, np.mean(motion_scores) * 20) if motion_scores else 0
        s_score = min(100, (np.mean(saturation_scores) / 255) * 100) if saturation_scores else 0
        return m_score, s_score

    def get_audio_score(self):
        try:
            if not os.path.exists(self.audio_path): return 0
            y, sr = librosa.load(self.audio_path, sr=22050)
            S = np.abs(librosa.stft(y))
            db = librosa.amplitude_to_db(S, ref=np.max)
            energy_idx = np.interp(np.mean(db), [-50, -5], [0, 100])
            onset_env = librosa.onset.onset_strength(y=y, sr=sr)
            tempo = librosa.feature.tempo(onset_envelope=onset_env, sr=sr)[0]
            tempo_idx = min(100, (tempo / 200) * 100)
            contrast = np.mean(librosa.feature.spectral_contrast(y=y, sr=sr))
            return min(100, (energy_idx * 0.4 + tempo_idx * 0.3 + (min(100, contrast * 4)) * 0.3))
        except: return 0

    def get_emotion_score(self):
        cap = cv2.VideoCapture(self.video_path)
        scores, count = [], 0
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret: break
            if count % 60 == 0:
                try:
                    res = DeepFace.analyze(frame, actions=['emotion'], enforce_detection=False, silent=True)
                    e = res[0]['emotion']
                    scores.append((e['happy'] * 1.0) + (e['surprise'] * 0.8) + (e['fear'] * 0.5))
                except: pass
            count += 1
        cap.release()
        return np.mean(scores) if scores else 50.0

    def analyze(self):
        has_audio = self._extract_audio()
        
        print("🎬 비디오 분석 시작...")
        m_score, s_score = self.get_visual_score()
        a_score = self.get_audio_score() if has_audio else 0
        e_score = self.get_emotion_score()
        
        print("🧠 AI 리콜 점수 계산 중...")
        r_score = self.get_ai_recall_score()
        
        # 최종 지수 계산 (리콜 점수 포함 가중치 조정)
        final_index = (m_score * 0.2) + (s_score * 0.05) + (a_score * 0.25) + (e_score * 0.25) + (r_score * 0.25)
        
        if os.path.exists(self.audio_path): os.remove(self.audio_path)
            
        result = {
            "dopamine_index": round(final_index, 2),
            "details": {
                "motion": round(m_score, 2),
                "saturation": round(s_score, 2),
                "audio": round(a_score, 2),
                "emotion": round(e_score, 2),
                "ai_recall": round(r_score, 2)
            }
        }
        return result

if __name__ == "__main__":
    VIDEO_FILE = "test.mp4"
    try:
        analyzer = DopamineAnalyzer(VIDEO_FILE)
        final_result = analyzer.analyze()
        print("\n" + "="*50)
        print(f"🚀 최종 도파민 지수: {final_result['dopamine_index']} / 100")
        print(f"📊 상세 분석: {final_result['details']}")
        print("="*50)
    except Exception as e:
        print(f"❌ 에러 발생: {e}")