"""Example answers for the graph page's Fill with demo values button.

Called by graph_tasks. A field with a lookup takes the first record the adapter returns, and the
rest are synthetic values the replica's own checks accept. It holds no secret value: a secret
field is never a requirement, so this file never needs one.
"""

from assistant.lookups import FixtureLookups, source_of
from assistant.navigator import Requirement

CONTACT = "رضا"
BILL = {"bill-id": "billId", "bill-pay-id": "payId", "wallet-op-bill": "billId", "wallet-op-pay": "payId"}

TYPED = {
    "account-transfer-amount": "250000",
    "account-transfer-reason": "0",
    "budget": "bills",
    "budget-limit": "2000000",
    "card-transfer-amount": "250000",
    "charity-amount": "100000",
    "cheque-no": "534001",
    "cheque-page": "25",
    "cheque-status": "PASSED",
    "color": "color-1",
    "conversion-identifier": "0000000000001",
    "conversion-mode": "ACCOUNT_TO_IBAN",
    "credit-amount": "100000",
    "delivery": "branch",
    "export-format": "PDF",
    "installment": "4",
    "loan-installment": "1",
    "field-address": "خیابان نمونه، پلاک ۱",
    "field-postal": "1234567890",
    "field-title": "خانه",
    "funding-amount": "1000000",
    "group-total": "300000",
    "schedule-end": "1405/12/01",
    "schedule-frequency": "DAILY",
    "schedule-start": "1405/09/01",
    "schedule-title": "اجاره نمونه",
    "source": "account-1",
    "statement-end": "1405/08/30",
    "statement-start": "1405/08/01",
    "svc-amount": "1000000",
    "svc-answer": "accept",
    "svc-code": "0000000019",
    "svc-branch": "central",
    "svc-document": "card",
    "svc-due": "1405/12/01",
    "svc-email": "demo@example.test",
    "svc-guarantor": CONTACT,
    "svc-guarantorCode": "0000000019",
    "svc-job": "کارمند",
    "svc-mobile": "09010000000",
    "svc-months": "12",
    "svc-points": "100",
    "svc-product": "personal",
    "svc-receiver": CONTACT,
    "svc-record": "plate-1",
    "toll-letter": "ب",
    "toll-plate-0": "12",
    "toll-plate-2": "345",
    "toll-plate-3": "67",
    "topup-amount": "200000",
    "topup-phone": "09010000000",
    "wallet-address": "address-1",
    "wallet-end": "1405/08/30",
    "wallet-op-amount": "50000",
    "wallet-op-mobile": "09010000000",
    "wallet-op-operator": "HAMRAH_AVAL",
    "wallet-start": "1405/08/01",
}

BY_SCREEN = {
    ("S-062", "svc-kind"): "saving",
    ("S-238", "svc-record"): "contact-1",
    ("S-247", "svc-record"): "contact-1",
    ("S-255", "svc-record"): "licence-1",
    ("S-164", "svc-kind"): "holder",
    ("S-162", "svc-sayad"): "0000000000000321",
    ("S-165", "svc-sayad"): "0000000000000999",
    ("S-308", "svc-amount"): "120000000",
    ("S-147", "cheque-kind"): "AUTHENTICATED_HOME_CHEQUE_FACILITIES_ISSUANCE_PAPER_CHECKBOOK",
    ("S-443", "inquiry-kind"): "credit-report",
    ("S-102", "wallet-op-amount"): "100000",
    ("S-148", "cheque-kind"): "AUTHENTICATED_HOME_CHEQUE_FACILITIES_TRANSFERRED_CHEQUES_REPORT",
}


def demo_value(item: Requirement, target: str, lookups: FixtureLookups) -> str:
    """An example answer, or an empty string when there is none."""
    typed = BY_SCREEN.get((target, item.ref)) or BY_SCREEN.get((item.screen, item.ref)) or TYPED.get(item.ref)
    if typed is not None:
        return typed
    if item.ref in BILL:
        return str(lookups.records("bills")[0][BILL[item.ref]])
    if item.lookup is None:
        return ""
    if source_of(item.lookup) == "contacts":
        return CONTACT
    return lookups.candidates(item.lookup)[0].key
