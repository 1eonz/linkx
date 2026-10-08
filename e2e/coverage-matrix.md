# E2E coverage matrix

This matrix describes the current evidence boundary. `Implemented` means a real source route and original SDK call are exercised by an offline Playwright test. `Manual` requires the native host window. `Planned` has no regression test yet.

| Target | Domain / route | Offline | Live/native | Evidence |
| --- | --- | --- | --- | --- |
| H5 | `/pages/createGroup` label based group | Implemented: success, mismatched/matching notification, cancel, API failure, license filter | Manual: native chat window | `h5portal/tests/group-flow.offline.spec.ts` |
| PC BSPC | `/statics` custom group | Implemented: `openSelectMemberUI` returned selection, SDK `createGroup`, `openChat`, API rejection | Manual: browser host selector/chat | `web-bspc/tests/statics-group.offline.spec.ts` |
| PC CSPC | `/statics?clientType=CSPC` custom group | Implemented: `selectMembers` direct result, SDK `createGroup`, `sms`, empty-result cancel | Manual: WebView2 native chat | `web-cspc/tests/statics-group.offline.spec.ts` |
| H5 | task list/detail and notification | Planned | Manual | Requires task fixtures and event contract |
| H5 | file, camera, GIS and media | Planned | Manual | Native permission/file chooser required |
| PC BSPC/CSPC | task, alarm, map and duty modules | Planned | Manual | Requires menu/data contracts per deployment |
| All | offline/online/theme/status/push events | Planned | Manual | Contract tests should be added before business expansion |

Synthetic users, groups and labels live next to their target fixtures. They are deliberately independent of internal test service accounts and must be replaced only with a documented test environment dataset when live tests are enabled.
