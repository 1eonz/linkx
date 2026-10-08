# PC CSPC statics group flow

This target uses the real WebView2 bridge SDK with `PIM_GetPlatform=win`. It verifies the real custom group entry, direct `selectMembers` result (the CSPC contract), conversion to the existing `createGroup` API payload, and the `sms` host method argument mapping. An empty member result is the cancellation path and must not create a group. Native WebView2 UI rendering remains a live host acceptance concern.
