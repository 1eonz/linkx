# PC BSPC statics group flow

This target starts the real `/statics` page and real BSPC bridge SDK. The scenario provides only the API responses needed by the existing login, permission, statistics and dialog initialization calls. The success case verifies the real `openSelectMemberUI` request and its returned selection payload, then checks the real `createGroup` and `openChat` calls. The failure case asserts the API error remains visible and no host chat call is made. The test uses synthetic E2E identities and must not be interpreted as native browser host rendering.
