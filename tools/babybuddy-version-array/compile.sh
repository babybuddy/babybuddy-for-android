#!/bin/bash

set -e

if [[ -f .wasbuilt ]]; then
  echo "Baby Buddy has already been built. Skipping."
  exit 0
fi

pipenv install --skip-lock -r requirements.txt 
export DJANGO_SETTINGS_MODULE=babybuddy.settings.development
pipenv run npm install
pipenv run npx gulp clean
pipenv run npx gulp build
pipenv run npx gulp collectstatic
pipenv run npx gulp makemigrations
pipenv run npx gulp migrate
pipenv run python3 manage.py createcachetable
pipenv run npx gulp fake
pipenv run python3 manage.py shell <<EOL
from django.contrib.auth.models import User
User.objects.create_superuser(username='testuser', password='testuser', email='testuser@localhost.local')
EOL

touch .wasbuilt
