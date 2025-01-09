# Use an official Python runtime as a parent image
FROM python:3.9.19-bullseye

ENV STREAMLIT_CONNECT_APP_HOST=host.docker.internal
ENV STREAMLIT_CONNECT_APP_PORT=50051
ENV LOG_LEVEL=INFO

# Set the working directory in the container to /app
WORKDIR /app

# Copy required files
COPY requirements.txt /app/
COPY src /app/src

# Install any needed packages specified in requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Install OpenSSH and setup
#RUN apt-get update && apt-get install -y openssh-server && \
#    echo "root:LetMeInPlz2024" | chpasswd && \
#    sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
#    sed -i 's/#Port 22/Port 50022/' /etc/ssh/sshd_config && \
#    mkdir /var/run/sshd

# Application on port 8501
EXPOSE 8501

# SSH on port 50022
EXPOSE 50022

# Run sshd in background and the app
#CMD /usr/sbin/sshd -D & cd /app && streamlit run src/app/main.py
CMD cd /app && streamlit run src/app/main.py