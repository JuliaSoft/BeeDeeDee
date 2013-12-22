# This script synchronizes your compiled classes
# with a copy held on the Julia cloud. Only classes that
# changed from last synchronization are copied.
# Run this from the top level of the project:
# ./scripts/sync_into_cloud.sh

rsync -avzr -e "ssh" bin/* julia-cloud:BeeDeeDee/
