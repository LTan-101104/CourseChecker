package com.example.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.imports")
public class ImportProperties {

    private int maxPdfSizeBytes = 15 * 1024 * 1024;
    private int requestTimeoutSeconds = 20;
    private final Async async = new Async();

    public int getMaxPdfSizeBytes() { return maxPdfSizeBytes; }
    public void setMaxPdfSizeBytes(int maxPdfSizeBytes) { this.maxPdfSizeBytes = maxPdfSizeBytes; }
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
    public Async getAsync() { return async; }

    public static class Async {
        private int corePoolSize = 2;
        private int maxPoolSize = 4;
        private int queueCapacity = 50;
        private String threadNamePrefix = "import-worker-";

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public String getThreadNamePrefix() { return threadNamePrefix; }
        public void setThreadNamePrefix(String threadNamePrefix) { this.threadNamePrefix = threadNamePrefix; }
    }
}
