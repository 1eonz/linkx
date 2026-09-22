#/bin/bash
parse_image() {
    local image="$1"
    echo "$image" | awk -F ':' '{print $1, $2}' OFS=' '
}

find /data/install/ -type f -name "images.txt" -exec cat {} + | grep -v '^\s*$' | sort -u > /data/combined_images.txt
Combined_Images_Path="/data/combined_images.txt"
declare -a all_images
readarray -t combined_images <  "$Combined_Images_Path"
repositories=$( curl -k -s "https://imagerepo.td-tech.com:5001/v2/_catalog?n=1000" |grep -o '"repositories":\s*\[.*\]' | sed 's/"repositories":\s*//' | sed 's/^\[//;s/\]$//;s/"//g')
IFS=',' read -r -a repo_array <<< "$repositories"
index=0
for repo in "${repo_array[@]}"; do
	tags=$(curl -k -s "https://imagerepo.td-tech.com:5001/v2/$repo/tags/list" |grep -o '"tags":\s*\[.*\]' | sed 's/"tags":\s*//'| sed 's/^\[//;s/\]$//;s/"//g')
	IFS=',' read -r -a tag_array <<< "$tags"
	if [ ${#tag_array[@]} -eq 0 ]; then 
		echo "$repo have no tags"
	else
		for tag in "${tag_array[@]}"; do
			echo "$repo:$tag"
			all_images[$index]="$repo:$tag"
			index=$((index + 1))
		done
	fi
done

for image in "${all_images[@]}"; do
	read -r name tag <<< $(parse_image "$image")
		if [[ "${combined_images[@]}" =~ "${image}" ]]; then
			echo "${image} does not need to be deleted. "
		else
			hashcode=$(curl -k --location "https://imagerepo.td-tech.com:5001/v2/$name/manifests/$tag" --header 'Accept: application/vnd.docker.distribution.manifest.v2+json' --head 2>/dev/null | grep -i 'Docker-Content-Digest' | awk '{print $2}')
			hashcode=$(echo "$hashcode" | tr -d '\n\r ')
			curl -k -X DELETE "https://imagerepo.td-tech.com:5001/v2/${name}/manifests/${hashcode}"
			echo "${image} deleted."		
		fi
done

POD_NAME=$(kubectl get pod -n platform -l app=registry -o jsonpath='{.items[0].metadata.name}')
kubectl exec -t $POD_NAME -n platform -- /bin/registry garbage-collect /data/registry/config.yml
sleep 60
#kubectl exec $POD_NAME -n platform -- ps -ef | grep '/bin/registry serve /data/registry/config.yml' | grep -v grep | awk '$1 != 1 {print $1}' | xargs -r kubectl exec $POD_NAME -n platform -- kill
#kubectl exec $POD_NAME -n platform -- sh -c "/bin/registry serve /data/registry/config.yml > /dev/null 2>&1 &"
kubectl delete po $POD_NAME -n platform --force

exit 0