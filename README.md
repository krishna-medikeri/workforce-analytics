Workforce Analytics using Spark on k8s
==============================

Quick start project to develop Scala based containerized Spark application using Eclipse.
 

## Build procedure

* Checkout the master branch `https://github.com/krishna-medikeri/workforce-analytics.git`
* Change directory to `workforce-analytics`
* If you want to build a jar with all the dependencies,  execute `sbt`. 
  - On sbt console run `eclipse` and then `package`
  
Packaged jar file will be available at `<workforce-analytics>/target/scala-2.11/workforce-analytics_2.11-1.0.jar`

## Setup Spark on k8s

Follow the instructions from `https://github.com/olxgroup-oss/spark-on-k8s` to setup Spark on k8s.

## Run in client mode

Note: Complete Spark on k8s setup before running following commands.

cd <spark-on-k8s>

export SPARK_HOME=<spark-on-k8s>/tmp/spark 
export PATH=<spark-on-k8s>/bin:$SPARK_HOME:$PATH

$SPARK_HOME/bin/spark-submit \
    --master k8s://https://$(minikube ip):8443 \
    --deploy-mode client \
    --conf spark.kubernetes.container.image=$(./get_image_name.sh spark) \
    --jars <workforce-analytics>/jars/db2jcc.jar,<workforce-analytics>/jars/stocator-1.0.24-IBM-SDK-jar-with-dependencies.jar \
    --class "Assignment" \
    <workforce-analytics>/jars/workforce-analytics_2.11-1.0.jar
    
    
## Run in cluster mode
 
Mount home directory to minikube VM
./bin/minikube start --cpus=4 --memory=4000mb --vm-driver=$(MINIKUBE_VMDRIVER) --kubernetes-version=$(K8S_VERSION) --mount-string ${HOME}:/hosthome --mount

Update following properties in `clustermode-podspec-with-rbac.yaml`:
`spec.containers.image`
`volumes.hostPath.path`
`spark.kubernetes.container.image`

Deploy pod to k8s	
`./bin/kubectl apply -f clustermode-podspec-with-rbac.yaml` # make sure you check the contents of this file to understand better how it works

check the executor pods in another terminal window while running
`./bin/kubectl get pods -w`
	

