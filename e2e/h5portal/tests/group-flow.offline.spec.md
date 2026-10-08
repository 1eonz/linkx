# H5 label group business flow

- Target: `h5portal`, mode: `offline`; actual route `pages/createGroup`.
- Product sources: `H5Portal/src/pages/createGroup.vue`, `src/hooks/useGroupCreateNotify.js`, original `src/static/js/WeSpaceSDK.js`.
- Synthetic identities and department values come from `fixtures/group-scenario.ts`; no production token or personal data is used.
- API contracts: GET license and H5 permissions, POST globals, GET collaboration labels, POST label/createGroup. Responses use the existing `{code,msg,data}` wrapper.
- Success: no selection disables submit; selecting the label enables it; creation request contains owner, department, label and GIS; wrong host group ID cannot open chat; matching `onCooperationGroupCreate` leads to original SDK `sms`, `switchTab`, `close` and clears loading.
- Cancel: clear the selection and require submit disabled without a create request.
- Failure: rejected API produces visible error, restores submit and does not call chat or close.
- License: personnel-check label is filtered when `LINKXACF=0`.
- Evidence: Playwright trace/screenshot/video plus fixture API and SDK call attachments. Native chat rendering is outside offline validation; use live/manual host acceptance for that.
