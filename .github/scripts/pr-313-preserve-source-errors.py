from pathlib import Path

path = Path("tools/levyra-editorial/levyra_editorial/collector.py")
text = path.read_text(encoding="utf-8")
old = '''    raise ValueError(f"Collection '{collection_id}' could not be collected.") from last_error
'''
new = '''    if last_error is not None:
        raise last_error
    raise ValueError(f"Collection '{collection_id}' could not be collected.")
'''
count = text.count(old)
if count != 1:
    raise RuntimeError(f"collector error propagation: expected one match, found {count}")
path.write_text(text.replace(old, new, 1), encoding="utf-8")
print("Preserved original editorial source errors")
