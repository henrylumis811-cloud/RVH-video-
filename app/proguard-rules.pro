# RVH Video app-specific R8 rules.
# Android manifest components are referenced explicitly so release shrinking
# cannot remove their public entry points even if reachability analysis changes.

-keep class com.rvh.video.RvhVideoApp {
    public <init>();
}

-keep class com.rvh.video.PlaybackService {
    public <init>();
}
