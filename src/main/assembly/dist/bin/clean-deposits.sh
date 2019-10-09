#!/usr/bin/env bash
#
# Helper script to delete all deposits that are in <state> state and are older than <keep> days (default two weeks).
#

usage() {
    echo "Usage: clean-deposits <state> <host-name> <to-email> [<depositor-account>] [<keep>]"
    echo "       clean-deposits --help"
}

while true; do
    case "$1" in
        -h | --help) usage; exit 0 ;;
        *) break;;
    esac
done

STATE=$1
EASY_HOST=$2
TO=$3
EASY_ACCOUNT=$4
KEEP=$5
FROM=noreply@dans.knaw.nl
BCC=
TMPDIR=/tmp

if [[ "$TO" == "-" ]]; then
    TO=easy.applicatiebeheer@dans.knaw.nl
fi

if [[ "$EASY_ACCOUNT" == "-" ]]; then
    EASY_ACCOUNT=""
fi

if [[ "$KEEP" == "" ]]; then
   KEEP=14
fi

if [[ "$FROM" == "" ]]; then
    FROM_EMAIL=""
else
    FROM_EMAIL="-r $FROM"
fi

if [[ "$BCC" == "" ]]; then
    BCC_EMAILS=""
else
    BCC_EMAILS="-b $BCC"
fi

TO_EMAILS="$TO"

DATE=$(date +%Y-%m-%d)
REPORT_DELETED=${TMPDIR}/report-deleted-$STATE-deposits-${EASY_ACCOUNT:-all}-$DATE.csv

exit_if_failed() {
    local EXITSTATUS=$?
    if [[ $EXITSTATUS != 0 ]]; then
        echo "ERROR: $1, exit status = $EXITSTATUS"
        echo "Deleting $STATE deposits FAILED ($EASY_HOST $DATE). Contact the system administrator." |
        mail -s "$(echo -e "FAILED: $EASY_HOST Report: deleting $STATE deposits for ${EASY_ACCOUNT:-all depositors}\nX-Priority: 1")" \
             $FROM_EMAIL $BCC_EMAILS $TO_EMAILS
        exit 1
    fi
}

echo "Cleaning $STATE deposits for ${EASY_ACCOUNT:-all depositors}..."
if [ $STATE == "DRAFT" ]
then
    /opt/dans.knaw.nl/easy-manage-deposit/bin/easy-manage-deposit clean --data-only \
                              --keep $KEEP \
                              --state $STATE \
                              --new-state-label INVALID \
                              --new-state-description "abandoned draft, data removed" \
                              --force \
                              --output \
                              --do-update \
                              $EASY_ACCOUNT > $REPORT_DELETED
else
    /opt/dans.knaw.nl/easy-manage-deposit/bin/easy-manage-deposit clean --data-only \
                              --keep $KEEP \
                              --state $STATE \
                              --force \
                              --output \
                              --do-update \
                              $EASY_ACCOUNT > $REPORT_DELETED
fi
exit_if_failed "clean deposits failed"

echo "Cleaning completed, sending report"
echo "Report: deleted $STATE deposits for (${EASY_ACCOUNT:-all depositors}) on $EASY_HOST" | \
mail -s "Report: deleted $STATE deposits for (${EASY_ACCOUNT:-all depositors}) on $EASY_HOST" \
	 -a $REPORT_DELETED \
	 $BCC_EMAILS $FROM_EMAIL $TO_EMAILS
exit_if_failed "sending of e-mail failed"

echo "Remove generated report files..."
rm -f $REPORT_DELETED
exit_if_failed "removing generated report file failed"
