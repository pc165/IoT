source ./env/bin/activate 
# python -m pip install -r requirements.txt
python -m uvicorn main:app --host 0.0.0.0 --reload
