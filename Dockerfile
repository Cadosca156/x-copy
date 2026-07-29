# syntax=docker/dockerfile:1
FROM python:3.12-alpine
WORKDIR /code
ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
RUN apk add --no-cache gcc musl-dev linux-headers  # Installs `gcc` and other dependencies
COPY requirements.txt .
RUN pip install -r requirements.txt  # Installs the Python dependencies
COPY . .
EXPOSE 5000
CMD ["flask", "run", "--debug"]