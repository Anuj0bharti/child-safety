import os

os.environ["DATABASE_URL"] = "sqlite:///./test_child_safety.db"
os.environ["DEMO_API_KEY"] = "test-demo-key"
os.environ["AI_PROVIDER"] = "disabled"
