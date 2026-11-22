type InitialLoaderBridge = {
    done: () => void;
    fail: (message?: string) => void;
};

declare global {
    interface Window {
        __GRCONE_INITIAL_LOADER__?: InitialLoaderBridge;
    }
}

let readyReported = false;

export function markInitialAppReady(): void {
    if (readyReported || typeof window === "undefined") {
        return;
    }

    readyReported = true;
    window.__GRCONE_INITIAL_LOADER__?.done();
}

export function reportInitialAppBootstrapFailure(): void {
    if (typeof window === "undefined") {
        return;
    }

    window.__GRCONE_INITIAL_LOADER__?.fail();
}

