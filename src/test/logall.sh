#!/usr/bin/env bash

CWD=$(cd $(dirname $0) && pwd)


function log () { 
    java -javaagent:$CWD/../../build/wiretap.jar \
        -Dwiretap.recorder=BinaryHistoryLogger \
        -Dwiretap.outfolder=$CWD/output/$1 \
        -cp $CWD/resources/regression/classes $1
}

log Bensalem
log CounterDatarace
log Deadlock
log Huang14
log InnerClass
log LockDynamics
log NestedDeadlock
log WaitProblem
