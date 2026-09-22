import os
import sys

webDir=''

def prepareWebEnv():
	global webDir
	webDir = sys.path[0]+'/../cloudcmd-web'
	os.chdir(webDir)

def start():
	webDirs = os.listdir(webDir)
	print(webDirs)
	for dir in webDirs:
		if os.path.isdir(webDir+'/'+dir):
			print(dir)
			build(dir)


def build(dir):
	buildDir = webDir+'/'+dir
	os.chdir(buildDir)
#	if not os.path.exists(buildDir+'/node_modules'):
#	os.system('npm config set registry http://mirrors.tools.huawei.com/npm/')
	os.system('npm config set registry https://registry.npm.taobao.org/')
	os.system('npm cache clean -f')
	os.system('npm install')
	os.system('npm run build:prod')

prepareWebEnv()
start()
