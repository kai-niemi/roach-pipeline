#!/bin/bash

FILE=target/pipeline.jar
if [ ! -f "$FILE" ]; then
    chmod +x mvnw
    ./mvnw clean install
fi

fn_start_h2(){
java -jar target/pipeline.jar h2 verbose \
 $*
}

fn_start_crdb(){
java -jar target/pipeline.jar crdb verbose \
 $*
}

fn_start_tpcc(){
java -jar target/pipeline.jar \
--spring.profiles.active=crdb,verbose \
--spring.datasource.url=jdbc:postgresql://localhost:26257/pipeline?sslmode=disable \
--pipeline.template.source.url=jdbc:postgresql://localhost:26257/tpcc \
--pipeline.template.source.username=root \
--pipeline.template.source.password= \
--pipeline.template.target.url=jdbc:postgresql://localhost:26257/tpcc_copy?sslmode=disable \
--pipeline.template.target.username=root \
--pipeline.template.target.password= \
 $*
}

fn_start_tpcc_dev(){
java -jar target/pipeline.jar \
--spring.profiles.active=crdb,verbose,dev \
--spring.datasource.url=jdbc:postgresql://192.168.1.99:26257/pipeline?sslmode=disable \
--pipeline.template.source.url=jdbc:postgresql://192.168.1.99:26257/tpcc \
--pipeline.template.source.username=root \
--pipeline.template.source.password= \
--pipeline.template.target.url=jdbc:postgresql://192.168.1.99:26257/tpcc_copy?sslmode=disable \
--pipeline.template.target.username=root \
--pipeline.template.target.password= \
 $*
}

########################################
########################################

getopt=$1
shift

case "${getopt}" in
    a)
        fn_start_h2 $*
        ;;
    b)
        fn_start_crdb $*
        ;;
    c)
        fn_start_tpcc $*
        ;;
    d)
        fn_start_tpcc_dev $*
        ;;
    *)
    if [ -n "${getopt}" ]; then
        echo -e "Unknown command: $0 ${getopt}"
    fi
    echo -e "Usage: $0 [command]"
    echo -e "Pipeline Launcher"
    echo -e ""
    echo -e "Commands"
    {
        echo -e "a\t| Start with profiles h2 verbose"
        echo -e "b\t| Start with profiles crdb verbose"
        echo -e "c\t| Start with profiles crdb verbose and templates for TPC-C (see run.sh)"
    } | column -s $'\t' -t
esac