#!/bin/bash
# Cluster setup using roachprod, see https://github.com/cockroachdb/cockroach/tree/master/pkg/cmd/roachprod
#
# To reset without destroying/recreating cluster:
# roachprod run $CLUSTER "sudo killall -9 cockroach"
# roachprod wipe $CLUSTER --preserve-certs

cloud="aws"
crdb_version="v22.1.0"
nodes=4

fn_prompt_yes_no(){
	local prompt="$1"
	local initial="$2"

	if [ "${initial}" == "Y" ]; then
		prompt+=" [Y/n] "
	elif [ "${initial}" == "N" ]; then
		prompt+=" [y/N] "
	else
		prompt+=" [y/n] "
	fi

	while true; do
		read -e -p  "${prompt}" -r yn
		case "${yn}" in
			[Yy]|[Yy][Ee][Ss]) return 0 ;;
			[Nn]|[Nn][Oo]) return 1 ;;
		*) echo -e "Please answer yes or no." ;;
		esac
	done
}

if fn_prompt_yes_no "Create CRDB cluster?" N; then
  exit 0
fi

if [ "${cloud}" = "aws" ]; then
region=eu-central-1
roachprod create "$CLUSTER" --clouds=aws --aws-machine-type-ssd=c5d.4xlarge --geo --local-ssd --nodes=${nodes} \
--aws-zones=\
eu-central-1a,\
eu-central-1b,\
eu-central-1c,\
eu-central-1a
fi

if [ "${cloud}" = "gce" ]; then
region=europe-west1
roachprod create "$CLUSTER" --clouds=gce --gce-machine-type=n1-standard-16 --geo --local-ssd --nodes=${nodes} \
--gce-zones=\
europe-west1-a,\
europe-west1-b,\
europe-west1-c,\
europe-west1-a
fi

if [ "${cloud}" = "azure" ]; then
region=westeurope
roachprod create "$CLUSTER" --clouds=azure --azure-machine-type=Standard_DS4_v2 --geo --local-ssd --nodes=${nodes} \
--azure-locations=westeurope
fi

echo "----------------"
echo "Stage Binaries"
echo "----------------"

roachprod stage $CLUSTER release ${crdb_version}
roachprod put ${CLUSTER}:4 target/pipeline.jar

echo "----------------"
echo "Start Up Services"
echo "----------------"

roachprod start $CLUSTER:1-3 --sequential
roachprod admin --open --ips $CLUSTER:1-3

echo "---------------------"
echo "Creating database..."
echo "---------------------"

roachprod run ${CLUSTER}:1 './cockroach sql --insecure --host=`roachprod ip $CLUSTER:1` -e "CREATE DATABASE pipeline;"'

roachprod admin --ips $CLUSTER:1-${nodes}

echo "Cluster setup complete!"

exit 0
