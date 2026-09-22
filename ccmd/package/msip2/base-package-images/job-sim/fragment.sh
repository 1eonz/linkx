#version=$1
#registry_host=$2
#name=$3
#arch=$4
#template_file=$5
image_name="${registry_host}/${name}:${version}.${arch}"

cd "$(dirname "$0")" || exit 1
script_root=$(pwd)
job_name="${name}-$(echo "$version" | tr '[:upper:]' '[:lower:]')"

ctr -n=k8s.io image import ./job-sim.tar || exit 1
ctr -n=k8s.io images push -k "${image_name}" || exit 1

kubectl delete job "${job_name}" -n linkx || echo "nope"

kubectl create job "${job_name}" \
  --image="${image_name}" \
  -n linkx \
  -o=go-template-file \
  --template="${script_root}/${template_file}" \
  --dry-run=server > "${script_root}/index-job.yaml" || exit 1

kubectl apply -f "${script_root}/index-job.yaml" || exit 1

kubectl wait --for=condition=complete job/${job_name} -n linkx --timeout=240s || exit 1