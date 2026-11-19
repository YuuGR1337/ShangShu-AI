# -*- coding: utf-8 -*-
"""RAG 评测公共模块：配置、登录、HTTP 封装（仅标准库，无第三方依赖）。"""

import json
import os
import time
import urllib.error
import urllib.request

BASE_URL = os.environ.get("RAG_EVAL_BASE_URL", "http://localhost:8080")
USERNAME = os.environ.get("RAG_EVAL_USERNAME", "rageval")
PASSWORD = os.environ.get("RAG_EVAL_PASSWORD", "RagEval#2026")
MODEL_ID = os.environ.get("RAG_EVAL_MODEL_ID", "deepseek-chat")

REQUEST_TIMEOUT = 120  # 秒：QueryRewrite + Rerank 链路较慢，放宽超时


def api(method, path, token=None, csrf=None, body=None):
    """调用后端接口，返回 Result.data；code != 0 或非 2xx 时抛 RuntimeError。"""
    url = BASE_URL.rstrip("/") + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if csrf:
        headers["X-CSRF-TOKEN"] = csrf
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", errors="replace")[:500]
        raise RuntimeError(f"{method} {path} -> HTTP {e.code}: {detail}") from e
    if isinstance(payload, dict) and "code" in payload:
        if payload.get("code") != 0:
            raise RuntimeError(f"{method} {path} -> code={payload.get('code')}: {payload.get('message')}")
        return payload.get("data")
    return payload


def login():
    """登录获取令牌，返回 (accessToken, csrfToken)。后端 LoginRequestDTO 的账号字段名为 account。"""
    data = api("POST", "/api/v1/auth/login", body={"account": USERNAME, "password": PASSWORD})
    return data["accessToken"], data.get("csrfToken")


def chat_once(token, csrf, model_id, prompt):
    """调用 /api/v1/model/ping 做单次补全，返回模型文本输出（字段名做多候选兜底）。"""
    data = api("POST", "/api/v1/model/ping", token=token, csrf=csrf,
               body={"modelId": model_id, "prompt": prompt[:2000]})
    if isinstance(data, dict):
        for key in ("response", "output", "content", "text", "message"):
            value = data.get(key)
            if isinstance(value, str) and value.strip():
                return value.strip()
        return json.dumps(data, ensure_ascii=False)
    return str(data)


def timed(fn):
    """简单计时装饰器，返回 (结果, 耗时毫秒)。"""
    start = time.time()
    result = fn()
    return result, int((time.time() - start) * 1000)
