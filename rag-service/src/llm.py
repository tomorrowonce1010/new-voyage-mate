import os
import httpx
from dotenv import load_dotenv

load_dotenv()

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")


def answer_with_deepseek(question: str, context: str) -> str:
	"""Call DeepSeek's OpenAI-compatible Chat Completions API."""
	if not DEEPSEEK_API_KEY:
		raise RuntimeError("DEEPSEEK_API_KEY not set")
	payload = {
		"model": DEEPSEEK_MODEL,
		"messages": [
			{"role": "system", "content": "你是中文智能旅行助手。回答必须依据给定资料，若没有相关信息就说不知道。"},
			{"role": "user", "content": f"[参考资料]\n{context}\n\n[问题]\n{question}"},
		],
		"temperature": 0.2,
	}
	headers = {"Authorization": f"Bearer {DEEPSEEK_API_KEY}", "Content-Type": "application/json"}
	url = f"{DEEPSEEK_BASE_URL}/v1/chat/completions"
	with httpx.Client(timeout=60) as client:
		resp = client.post(url, headers=headers, json=payload)
		resp.raise_for_status()
		data = resp.json()
		return data["choices"][0]["message"]["content"].strip()
