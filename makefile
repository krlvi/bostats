frontend-deploy: frontend-build frontend-s3-sync

frontend-build:
	lein do clean, cljsbuild once min

frontend-s3-sync:
	aws s3 sync ./resources/public/ s3://aws-website-videlovcom-n7zfb/

backend-deploy: backend-build backend-lambda-deploy

backend-build:
	lein uberjar

backend-lambda-deploy:
	aws lambda update-function-code \
		--region eu-west-1 \
		--function-name bostad-stats \
		--zip-file fileb://./target/uberjar/bostats-0.1.0-SNAPSHOT-standalone.jar