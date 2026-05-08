import cv2
import numpy as np
import librosa
import os
from deepface import DeepFace

class DopamineAnalyzer:
    def __init__(self, video_path, audio_path):
        """
        초기화 및 파일 경로 확인
        """
        self.video_path = video_path
        self.audio_path = audio_path
        
        if not os.path.exists(video_path):
            raise FileNotFoundError(f"비디오 파일을 찾을 수 없습니다: {video_path}")
        if not os.path.exists(audio_path):
            raise FileNotFoundError(f"오디오 파일을 찾을 수 없습니다: {audio_path}")
            
    def get_visual_score(self):
        """
        1. Optical Flow를 통한 실제 움직임 벡터 추출
        2. 화면의 채도(Saturation) 분석
        """
        cap = cv2.VideoCapture(self.video_path)
        motion_scores = []
        saturation_scores = []
        
        ret, prev_frame = cap.read()
        if not ret: 
            cap.release()
            return 0, 0
        
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
        """
        에너지 분석 로직을 로그 스케일(dB)로 수정하여 변별력 확보
        """
        try:
            sr = 22050
            y, _ = librosa.load(self.audio_path, sr=sr)
            
            if len(y) == 0:
                return 0
            
            # 1. 에너지(dB) 계산: 무작정 1000을 곱하는 대신 현실적인 데시벨 범위 사용
            S = np.abs(librosa.stft(y))
            db = librosa.amplitude_to_db(S, ref=np.max)
            avg_db = np.mean(db)
            # 보통 음악의 평균 dB는 -40 ~ -10 사이입니다. 이를 0~100점으로 매핑
            energy_idx = np.interp(avg_db, [-50, -5], [0, 100])
            
            # 2. 템포(BPM) 계산: 200 BPM을 100점 기준으로 정규화
            onset_env = librosa.onset.onset_strength(y=y, sr=sr)
            tempo_list = librosa.feature.tempo(onset_envelope=onset_env, sr=sr)
            tempo = tempo_list[0] if len(tempo_list) > 0 else 0
            tempo_idx = min(100, (tempo / 200) * 100)
            
            # 3. 주파수 대비: 일반적인 음악 범위를 100점 기준으로 정규화
            contrast = librosa.feature.spectral_contrast(y=y, sr=sr)
            contrast_idx = min(100, np.mean(contrast) * 4) # 가중치 조정
            
            # 최종 합산 (에너지 40%, 템포 30%, 대비 30%)
            final_audio_score = (energy_idx * 0.4 + tempo_idx * 0.3 + contrast_idx * 0.3)
            return min(100, final_audio_score)

        except Exception as e:
            print(f"\n[오디오 분석 에러] 상세 에러: {e}")
            return 0

    def get_emotion_score(self):
        """
        DeepFace를 이용한 각성도 분석
        """
        cap = cv2.VideoCapture(self.video_path)
        scores = []
        count = 0
        
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret: break
            
            if count % 60 == 0:
                try:
                    res = DeepFace.analyze(frame, actions=['emotion'], enforce_detection=False, silent=True)
                    e = res[0]['emotion']
                    # 각성 감정 합산
                    arousal = (e['happy'] * 1.0) + (e['surprise'] * 0.8) + (e['fear'] * 0.5)
                    scores.append(arousal)
                except:
                    pass
            count += 1
            
        cap.release()
        return np.mean(scores) if scores else 50.0

    def analyze(self):
        print(f"--- 분석 시작: {os.path.basename(self.video_path)} ---")
        
        m_score, s_score = self.get_visual_score()
        a_score = self.get_audio_score()
        e_score = self.get_emotion_score()
        
        # 가중치: 움직임 30%, 채도 10%, 오디오 30%, 감정 30%
        final_index = (m_score * 0.3) + (s_score * 0.1) + (a_score * 0.3) + (e_score * 0.3)
        
        print("\n" + "="*40)
        print(f"{'분석 항목':<20} | {'점수':>10}")
        print("-" * 40)
        print(f"{'1. Motion Velocity':<20} | {m_score:>10.2f}")
        print(f"{'2. Visual Saturation':<20} | {s_score:>10.2f}")
        print(f"{'3. Audio Stimulation':<20} | {a_score:>10.2f}")
        print(f"{'4. Emotional Intensity':<20} | {e_score:>10.2f}")
        print("-" * 40)
        print(f"{'▶ 최종 도파민 지수':<20} | {final_index:>10.2f} / 100")
        print("="*40)
        
        return final_index

if __name__ == "__main__":
    VIDEO_FILE = "king.mp4"
    AUDIO_FILE = "kings.mp3"
    
    try:
        analyzer = DopamineAnalyzer(VIDEO_FILE, AUDIO_FILE)
        analyzer.analyze()
    except Exception as e:
        print(f"실행 에러: {e}")