cd bin; jar -cf beedeedee.jar com/*; scp beedeedee.jar julia-cloud:julia/lib/;rm beedeedee.jar; cd ..; ./scripts/sync_into_cloud.sh
