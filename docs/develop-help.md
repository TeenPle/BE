### `환경변수 & 파일 보안`
프로젝트 보안사항 관련된 파일들을 keys 디렉토리 하위로 이동시킨다.
application.yaml에선 해당 경로 내의 환경변수 또는 파일을 읽는다.  
ex) `.env`, `firebase-service-account.json`, etc..

### `로컬(개발) 서버 환경 통일`
docker compose를 사용하여 각자의 컴퓨터에서 환경을 통일한다.
프로젝트 root경로(compose.yml파일이 있는 디렉토리)에서 아래의 명령어를 실행한다.  
```shell
$ docker-compose up -d
```