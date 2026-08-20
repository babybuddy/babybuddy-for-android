#!/bin/bash

set -e

if [[ -f .wasbuilt ]]; then
  echo "Baby Buddy has already been built. Skipping."
  exit 0
fi

PY_VER_ARG=
if [[ -n "$PYTHON_VERSION" ]]; then
  PY_VER_ARG="--python $PYTHON_VERSION"
fi

pipenv install $PY_VER_ARG --skip-lock -r requirements.txt 

# Weird requirement needed to make distutils work for newer Python versions.
pipenv install $PY_VER_ARG --skip-lock setuptools

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
