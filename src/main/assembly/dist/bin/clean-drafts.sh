#!/usr/bin/env bash
#
# Helper script to delete all deposits that are in DRAFT state and are older than two weeks.
#


usage() {
    echo "Usage: clean-draft-deposits"
    echo "       clean-draft-deposits --help"
}


while true; do
    case "$1" in
        -h | --help) usage; exit 0 ;;
        *) break;;
    esac
done

EASY_ACCOUNT=$1
TO=easy.applicatiebeheer@dans.knaw.nl
FROM=noreply@dans.knaw.nl
BCC=
TMPDIR=/tmp

if [[ "$EASY_ACCOUNT" == "-" ]]; then
    EASY_ACCOUNT=""
fi

DATE=$(date +%Y-%m-%d)
REPORT_DELETED=${TMPDIR}/report-deleted-drafts-${EASY_ACCOUNT:-all}-$DATE.csv

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

exit_if_failed() {
    local EXITSTATUS=$?
    if [[ $EXITSTATUS != 0 ]]; then
        echo "ERROR: $1, exit status = $EXITSTATUS"
        echo "Deleting DRAFT deposits FAILED. Contact the system administrator." |
        mail -s "$(echo -e "FAILED: $EASY_HOST Report: deleting DRAFT deposits for ${EASY_ACCOUNT:-all depositors}\nX-Priority: 1")" \
             $FROM_EMAIL $BCC_EMAILS $TO_EMAILS
        exit 1
    fi
}

echo  "Cleaning deposits for ${EASY_ACCOUNT:-all depositors}..."
/opt/dans.knaw.nl/easy-manage-deposit/bin/easy-manage-deposit clean --data-only \
						  --keep 14 \
						  --state DRAFT \
						  --new-state-label INVALID \
						  --new-state-description "abandoned draft, data removed" \
						  --force \
						  --output \
						  --do-update \
						  $EASY_ACCOUNT > $REPORT_DELETED
exit_if_failed "clean deposits failed"

echo "Report: deleted DRAFT deposits for (${EASY_ACCOUNT:-all depositors})" | \
mail -s "Report: deleted DRAFT deposits for (${EASY_ACCOUNT:-all depositors})" \
	 -a $REPORT_DELETED \
	 $BCC_EMAILS $FROM_EMAIL $TO_EMAILS
exit_if_failed "sending of e-mail failed"

rm -f $REPORT_DELETED
