#!/usr/bin/sh
echo para:$@
base_branch=$1
future_resource=$2
future_branch=$3
git_local=$4
base_resource=$future_resource

set -x

if [ $# -ne 4 ];then
	echo "input para number is error"
	echo "====================================================================================================================="
	echo "=== ./timer_pull_rebase.sh ${base_resource} ${base_branch} ${future_resource} ${future_branch} ${git_local} ===="
	echo "====================================================================================================================="
	exit 1
fi



git_pull(){
	pull_cmd=""
	echo "pull_cmd: ${pull_cmd}"
	cd ${git_local} && git clean -dxf && git checkout . && git fetch --all && git checkout ${future_branch} && git pull || { echo "git pull is failed"; return 1; }
}


git_checkout()
{
	checkout_cmd=""
	echo "=============checkout command: ${checkout_cmd}"
	cd 
	git lfs clone -b ${future_branch} ${future_resource} ${git_local} || { echo "git checkout is failed"; return 1; }
}

git_download(){

	if [ -d ${git_local}/.git ];then
		if git_pull;then
			echo "git pull is successful"
		else
			rm -rv ${git_local}
			git_checkout  || return 1 
		fi
	else
		git_checkout || return 1 
	fi
}


git_rebase()
{
	rebase_cmd="cd ${git_local} && git pull --tags origin ${future_branch} && git pull --tags origin ${base_branch}"
	echo "=============rebase command: ${rebase_cmd}"
	cd ${git_local} && git pull --tags origin ${future_branch} && git pull --tags origin ${base_branch} -Xignore-all-space || { echo "git rebase is failed"; return 1; }

}

abort_merge()
{
	cd ${git_local} 
	git merge --abort
	git clean -dxf
	git checkout .
	git pull
}

git_push()
{

	push_cmd="git pull --tags origin ${future_branch} && git push origin ${future_branch}"
	cd ${git_local}
	git pull --tags origin ${future_branch} && git push origin ${future_branch} || { echo "git push is failed"; return 1; }

}


main(){

	git_download && echo "git_download is successful" || { echo "git_download is failed"; exit 1; }
	if git_rebase; then
		git_push && echo "git_push is successful"  || { echo "git_push is failed"; exit 1; }
	else
		abort_merge
		echo "trunk code pull rebase failed"
		exit 1
	fi
}

main
