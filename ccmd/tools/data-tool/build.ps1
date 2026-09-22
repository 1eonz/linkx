docker build -f Dockerfile-arm64 --platform=linux/arm64 -t data-tool .
Set-Location pkg
docker save -o data-tool.s data-tool
tar -zcvf data-tool.tar.gz data-tool.s dump.sh restore.sh tools.sh
Set-Location ..
docker rmi data-tool
docker buildx prune --force