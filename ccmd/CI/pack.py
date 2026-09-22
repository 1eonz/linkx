import os
import sys

ciPath=''
version='1.0.0-SNAPSHOT'

def getCcmdPkgName():
	return 'ccmd-'+version+'.tar.gz'
	
def createTmpPath():
	global ciPath
	ciPath = sys.path[0]
	tmpPath = ciPath+'/tmp'
	if os.path.isdir(tmpPath):
		os.system('rm -rf '+tmpPath)
	os.system('mkdir -p '+tmpPath)

def getDirs(rootDir,targetDir):
	dirs = []
	list_files = os.listdir(rootDir)
	for file in list_files:
		currDir = rootDir+os.sep+file
		if os.path.isdir(currDir):
			if file==targetDir:
				dirs.append(currDir)
			else:
				tmp_dirs = getDirs(currDir,targetDir)
				dirs.extend(tmp_dirs)
	return dirs	
				


def genCcmdPkg():
	cloudcmd = 'cloudcmd'
	if os.path.exists(cloudcmd):
		os.system('rm -rf '+cloudcmd)
	os.system('mkdir -p '+cloudcmd+'/lib')
	service_dirs = getDirs('../cloudcmd-service','target')
	framework_dirs = getDirs('../cloudcmd-framework','target')
	service_dirs.extend(framework_dirs)
	for dir in service_dirs:
		service_path = os.path.dirname(dir)
		service_name = os.path.basename(service_path)
		service_version_name = service_name+'-'+version
		service_version_path = dir+os.sep+service_version_name+os.sep+service_version_name		
		print(service_version_path)
		service_bin_path = service_path+os.sep+'bin'
		ccmd_service_path = cloudcmd+'/services/'+service_name
		if os.path.exists(service_bin_path):
			os.system('mkdir -p '+ccmd_service_path)
			os.system('cp -rf '+service_bin_path+' '+ccmd_service_path)
		if os.path.exists(service_version_path):
			os.system('cp -rf '+service_version_path+os.sep+'*.jar '+ccmd_service_path)
			target_dirs = os.listdir(service_version_path)
			for target_dir in target_dirs:
				target_dir_abs_path = service_version_path+os.sep+target_dir
				if target_dir == "lib":
					os.system('cp -rf '+target_dir_abs_path+' '+cloudcmd+'/')
				else:
					os.system('cp -rf '+target_dir_abs_path+' '+ccmd_service_path)
						
	
	install_path = ciPath+'/../install'
	os.system('cp -rf '+install_path+'/cloudcmd/* '+cloudcmd)	
	os.system('cp -rf '+install_path+'/*.py '+cloudcmd)	
	os.system('cp -rf '+install_path+'/version.icpx.ini '+cloudcmd)	
	os.system('cp -rf '+install_path+'/cloudcmd_setup.sh '+cloudcmd)
	os.system('cp -rf '+install_path+'/uninstall.sh '+cloudcmd)	
	os.system('mkdir '+cloudcmd+'/dump')
#	os.system('tar zcvf ccmd-'+version+'.tar.gz '+cloudcmd)


def copyInstallFiles():
	tmpPath = ciPath+'/tmp'
	os.system('cp ../install/isp/ISP_BASIC.tar.gz '+tmpPath)
#	os.system('cp ../cloudcmd-pack/target/*.tar.gz '+tmpPath)
	os.system('cp ../install/pack.list '+tmpPath)
	os.system('cp ../install/name.txt '+tmpPath)
	os.system('cp ../install/sca_install.sh '+tmpPath)
	os.system('cp ../install/setup_ch.sh '+tmpPath)
	os.system('cp ../install/setup_en.sh '+tmpPath)

def createAdapterPkg():
	os.system('cp -rf ../install/cloud_adapter .')
	os.system('tar zcvf tmp/cloud_adapter.tar.gz cloud_adapter')
	os.system('rm -rf cloud_adapter')
	
def copyCloudcmdWeb():
	webSrc='../../web/cloudcmd'
	dst='cloudcmd/web/cloudcmd-web'
	os.system('cp -rf '+webSrc +' '+ dst)

	resSrc='../cloudcmd-static'
	resdst='cloudcmd/web/cloudcmd-static'
	os.system('cp -rf '+resSrc +' '+ resdst)



   
def copyCloudcmdPatrol():
	webSrc='../../../../ICP-X/patrol/trunk/patrol'
	dst='cloudcmd/web/patrol-web'
	os.system('cp -rf '+webSrc +' '+ dst)

def addWebPkgToCcmd():
	tmpPath = ciPath+'/tmp'
	ccmdFileName = getCcmdPkgName()
	os.system('mkdir -p cloudcmd/web')
	webDir = ciPath+'/../cloudcmd-web'
	for dir in os.listdir(webDir):
		absDir = webDir+'/'+dir
		if os.path.isdir(absDir):
			distDir = absDir+'/dist'
			print('distdir:'+distDir)
			os.system('cp -rf '+distDir+' cloudcmd/web/'+dir)
	copyCloudcmdWeb()
	copyCloudcmdPatrol()

	os.system('cp -rf '+ciPath+'/../install/cloudcmd/nginx cloudcmd/') #copy nginx config
	os.system('tar zcvf '+ccmdFileName+' cloudcmd')
	os.system('cp -f '+ccmdFileName+ ' '+tmpPath)
	os.system('rm -rf cloudcmd')

def addToPackList():
	tmpPath = ciPath+'/tmp'
	ccmdFileName = getCcmdPkgName()
	os.system('echo \"'+ccmdFileName+' cloudcmd/cloudcmd_setup.sh NA\" >>'+tmpPath+'/pack.list')


def genShaFile():
	tmpPath = ciPath+'/tmp'
	tmpShaFileName = ciPath+'/tmp.sha512'
	tmpShaFile = open(tmpShaFileName,'w')
	count = 0	
	for file in os.listdir(tmpPath):
		absFileName = tmpPath+'/'+file
		cmdRet = os.popen('sha512sum '+absFileName)
		shasumLine = cmdRet.read()
		print(shasumLine)
		shasum = shasumLine.split(' ',1)
		shasumLineNew = shasum[0]+'  '+file
		if count == 0:
			tmpShaFile.write(shasumLineNew)
		else:
			tmpShaFile.write('\n'+shasumLineNew)
		count += 1	
	tmpShaFile.close()
	os.system('mv '+tmpShaFileName+' '+tmpPath+'/eApp.sha512')
	
	
def genInstallPkg():
	tmpPath = ciPath+'/tmp'
	cmd = 'find '+tmpPath+' -type f -printf \'%P\\0\'|tar czvf '+ciPath+'/cloudcmd.tar.gz'+' -C '+tmpPath+' --null -T -'
	os.system(cmd)

def package():
	createTmpPath()
	genCcmdPkg()
	copyInstallFiles()
	createAdapterPkg()
	addWebPkgToCcmd()
	addToPackList()
	genShaFile()
	genInstallPkg()

package()
