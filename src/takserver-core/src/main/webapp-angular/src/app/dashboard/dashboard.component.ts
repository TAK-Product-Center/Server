import { Component } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import { LegendPosition } from '@swimlane/ngx-charts';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, NgxChartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {

  private serverVersionApiUrl = '/Marti/api/version';
  private serverNodeIdApiUrl = '/Marti/api/node/id';
  private memoryMetricsApiUrl = '/actuator/custom-memory-metrics';
  private networkMetricsApiUrl = '/actuator/custom-network-metrics';
  private queueMetricsApiUrl = '/actuator/custom-queue-metrics';
  private cpuMetricsApiUrl = '/actuator/custom-cpu-metrics';
  private startTimeApiUrl = '/actuator/metrics/process.start.time';
  private dbMetricsApiUrl = '/actuator/takserver-database';
  private diskMetricsApiUrl = '/actuator/custom-disk-metrics';

  rowCollapsed: boolean[] = [false, false, false, false];

  takVersion = "0";
  nodeId = "0";
  messagingCpuUtilized: number = 0;
  messagingCpuCores: number = 0;
  messagingCpuUsagePercentage: number = 0;
  messagingCpuUsageData: any[] = [];

  private lastNetSample?: {
    tMs: number;
    reads: number;
    writes: number;
    bytesRead: number;
    bytesWritten: number;
  };

  messagingHeapMemoryData: { name: string; series: { name: string; value: number }[] }[] = [
    { name: 'Messaging Heap Allocated', series: [] },
    { name: 'Messaging Heap Utilized', series: [] }
  ];
  messagingHeapChartYScaleMax = 2000;

  heapMemoryData: { name: string; series: { name: string; value: number }[] }[] = [
    { name: 'Heap Allocated', series: [] },
    { name: 'Heap Utilized', series: [] }
  ];

  writesPerSecondData: { name: string; series: { name: string; value: number}[] }[] = [
    { name: 'Writes Per Second', series: [] }
  ]

  readsPerSecondData: { name: string; series: { name: string; value: number}[] }[] = [
    { name: 'Reads Per Second', series: [] }
  ]

  bytesWrittenPerSecondData: { name: string; series: { name: string; value: number}[] }[] = [
    { name: 'Bytes Written Per Second', series: [] }
  ]

  bytesReadPerSecondData: { name: string; series: { name: string; value: number}[] }[] = [
    { name: 'Bytes Read Per Second', series: [] }
  ]

  totalCores: number = 0;
  cpuUtilized: number = 0;
  cpuUsagePercentage: number = 0;
  cpuUsageData: any[] = [];

  serverStartTime = "N/A";
  serverUpTime = "N/A";

  submissionQueueCurrentSize = 0;
  submissionQueueCapacity = 0;
  submissionQueueUsageData: any[] = [];

  brokerQueueCurrentSize = 0;
  brokerQueueCapacity = 0;
  brokerQueueUsageData: any[] = [];

  repositoryQueueCurrentSize = 0;
  repositoryQueueCapacity = 0;
  repositoryQueueUsageData: any[] = [];

  diskUsedMB: number = 0;
  diskTotalMB: number = 0;
  diskUsableMB: number = 0;
  diskUsedPercentage: number = 0;

  colorScheme: Color = {
    domain: ['#ff7300', '#007bff'], // Colors for Heap Utilized and Heap Allocated
    group: ScaleType.Ordinal,
    selectable: true,
    name: 'customScheme'
  };

  colorScheme2: Color = {
    domain: ['#ff7300'],
    group: ScaleType.Ordinal,
    selectable: true,
    name: 'customScheme2'
  };

  colorScheme3: Color = {
    domain: ['#007bff'],
    group: ScaleType.Ordinal,
    selectable: true,
    name: 'customScheme3'
  };

  heapChartYScaleMax = 2000;
  writesYScaleMax = 2000;
  readsYScaleMax = 2000;
  bytesWrittenYScaleMax = 2000;
  bytesReadYScaleMax = 2000;

  legendPosition = LegendPosition.Below;

  useDummyData = false;

  initializeData() {
    const initialData = [];
    for (let i = 0; i < this.maxEntries; i++) {
      const timestamp = 45 - (i * this.updateIntervalS);
      initialData.push({ name: `${timestamp} s`, value: 0 });
    }

    this.heapMemoryData = [
      { name: 'Heap Allocated', series: [...initialData] },
      { name: 'Heap Utilized', series: [...initialData] }
    ];

    this.writesPerSecondData = [
      { name: 'Writes Per Second', series: [...initialData] }
    ]

    this.readsPerSecondData = [
      { name: 'Reads Per Second', series: [...initialData] }
    ]

    this.bytesWrittenPerSecondData = [
      { name: 'Bytes Written Per Second', series: [...initialData] }
    ]

    this.bytesReadPerSecondData = [
      { name: 'Bytes Read Per Second', series: [...initialData] }
    ]

    this.messagingHeapMemoryData = [
      { name: 'Messaging Heap Allocated', series: [...initialData] },
      { name: 'Messaging Heap Utilized', series: [...initialData] }
    ];
  }

  totalConnections = -1;

  view: [number, number] = [500, 250];
  gaugeView: [number, number] = [400, 280]; // Width, height
  xAxis = true;
  yAxis = true;
  legend = false;
  xAxisLabel = 'Time';
  heapChartYAxisLabelHeap = 'Memory (MB)';
  writesChartYAxisLabelHeap = 'Writes Per Second';
  readsChartYAxisLabelHeap = 'Reads Per Second';
  bytesWrittenChartYAxisLabelHeap = 'Bytes Written Per Second';
  bytesReadChartYAxisLabelHeap = 'Bytes Read Per Second';

  private globalMetricsApiUrl = '/fig/getBrokerGlobalMetrics';
  private metricsApiUrl = '/fig/getBrokerMetrics';

  private updateInterval: any;
  private updateIntervalMs = 5000; // update interval in milliseconds
  private updateIntervalS = this.updateIntervalMs / 1000; // update interval in seconds
  private maxEntries = 10;

  private yAxisBuffer = .3;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    console.log('ng on init called for dashboard');

    this.initializeData();

    if (this.useDummyData) {
      console.log('Using dummy data mode');
      this.fetchDummyMetrics();
      this.updateInterval = setInterval(() => this.fetchDummyMetrics(), this.updateIntervalMs);
    } else {
      console.log('Using real API data mode');
      this.fetchAllMetrics();
      this.fetchServerInfo();
      this.updateInterval = setInterval(() => this.fetchAllMetrics(), this.updateIntervalMs);
    }
  }

  ngOnDestroy() {
    // Clear the interval when the component is destroyed to prevent memory leaks
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }
  }

  fetchAllMetrics() {
    this.fetchMemoryMetrics();
    this.fetchNetworkMetrics();
    this.fetchQueueMetrics();
    this.fetchCpuMetrics();
    this.fetchStartTime();
    this.fetchDatabaseMetrics();
    this.fetchDiskMetrics();
    this.cdr.detectChanges();
  }

  fetchServerInfo() {
    this.http.get(this.serverVersionApiUrl, { responseType: 'text' }).subscribe(
      data => {
        this.takVersion = data || "Unknown";
      },
      error => console.error('Error fetching server version:', error)
    );

    this.http.get(this.serverNodeIdApiUrl, { responseType: 'text' }).subscribe(
      data => {
        this.nodeId = data || "Unknown";
      },
      error => console.error('Error fetching server node ID:', error)
    );
  }

  fetchMemoryMetrics() {
    this.http.get<any>(this.memoryMetricsApiUrl).subscribe(
      data => {
        const heapCommitted = data.heapCommitted / 1_000_000;
        const heapUsed = data.heapUsed / 1_000_000;
        const messagingHeapCommitted = data.messagingHeapCommitted / 1_000_000;
        const messagingHeapUsed = data.messagingHeapUsed / 1_000_000;

        this.updateHeapData(heapUsed, heapCommitted);
        this.updateMessagingHeapData(messagingHeapUsed, messagingHeapCommitted);
      },
      error => console.error('Error fetching memory metrics:', error)
    );
  }

  fetchDiskMetrics() {
    this.http.get<any>(this.diskMetricsApiUrl).subscribe(
      data => {
        const bytesToMB = (b: number) => Math.round(b / 1_000_000); // MB

        this.diskTotalMB = bytesToMB(data.totalSpace);
        this.diskUsedMB = bytesToMB(data.usedSpace);
        this.diskUsableMB = bytesToMB(data.usableSpace);
        this.diskUsedPercentage = Math.round((data.usedSpace / data.totalSpace) * 100);
      },
      error => console.error('Error fetching disk metrics:', error)
    );
  }

  fetchNetworkMetrics() {
    this.http.get<any>(this.networkMetricsApiUrl).subscribe(
      data => {
        const nowMs = Date.now();

        const curr = {
          writes: Number(data.numWrites ?? 0),
          reads: Number(data.numReads ?? 0),
          bytesWritten: Number(data.bytesWritten ?? 0),
          bytesRead: Number(data.bytesRead ?? 0),
          clientsConnected: Number(data.numClients ?? 0),
        };

        if (!this.lastNetSample) {
          this.lastNetSample = { tMs: nowMs, reads: curr.reads, writes: curr.writes, bytesRead: curr.bytesRead, bytesWritten: curr.bytesWritten };

          this.pushNetworkRatesToCharts(0, 0, 0, 0);
          this.totalConnections = curr.clientsConnected;
          return;
        }

        const dtSec = Math.max(0.001, (nowMs - this.lastNetSample.tMs) / 1000);

        const d = {
          writes: Math.max(0, curr.writes - this.lastNetSample.writes),
          reads: Math.max(0, curr.reads - this.lastNetSample.reads),
          bytesWritten: Math.max(0, curr.bytesWritten - this.lastNetSample.bytesWritten),
          bytesRead: Math.max(0, curr.bytesRead - this.lastNetSample.bytesRead),
        };

        const writesPerSec = d.writes / dtSec;
        const readsPerSec = d.reads / dtSec;
        const bytesWrittenPerSec = d.bytesWritten / dtSec;
        const bytesReadPerSec = d.bytesRead / dtSec;

        this.pushNetworkRatesToCharts(writesPerSec, readsPerSec, bytesWrittenPerSec, bytesReadPerSec);
        this.totalConnections = curr.clientsConnected;

        this.lastNetSample = { tMs: nowMs, reads: curr.reads, writes: curr.writes, bytesRead: curr.bytesRead, bytesWritten: curr.bytesWritten };
      },
      error => console.error('Error fetching network metrics:', error)
    );
  }

  private pushNetworkRatesToCharts(
    writesPerSec: number,
    readsPerSec: number,
    bytesWrittenPerSec: number,
    bytesReadPerSec: number
  ) {
    const writesResult = this.updateTimeSeriesData(this.writesPerSecondData, writesPerSec, 'Writes Per Second', this.updateIntervalS, this.yAxisBuffer);
    this.writesPerSecondData = writesResult.updatedData;
    this.writesYScaleMax = writesResult.newYScaleMax;

    const readsResult = this.updateTimeSeriesData(this.readsPerSecondData, readsPerSec, 'Reads Per Second', this.updateIntervalS, this.yAxisBuffer);
    this.readsPerSecondData = readsResult.updatedData;
    this.readsYScaleMax = readsResult.newYScaleMax;

    const bytesWrittenResult = this.updateTimeSeriesData(this.bytesWrittenPerSecondData, bytesWrittenPerSec, 'Bytes Written Per Second', this.updateIntervalS, this.yAxisBuffer);
    this.bytesWrittenPerSecondData = bytesWrittenResult.updatedData;
    this.bytesWrittenYScaleMax = bytesWrittenResult.newYScaleMax;

    const bytesReadResult = this.updateTimeSeriesData(this.bytesReadPerSecondData, bytesReadPerSec, 'Bytes Read Per Second', this.updateIntervalS, this.yAxisBuffer);
    this.bytesReadPerSecondData = bytesReadResult.updatedData;
    this.bytesReadYScaleMax = bytesReadResult.newYScaleMax;
  }

  fetchQueueMetrics() {
    this.http.get<any>(this.queueMetricsApiUrl).subscribe(
      data => {
        this.repositoryQueueCapacity = data.brokerCapacity;
        this.repositoryQueueCurrentSize = data.brokerSize;
        this.brokerQueueCapacity = data.repositoryCapacity;
        this.brokerQueueCurrentSize = data.repositorySize;
        this.submissionQueueCapacity = data.submissionCapacity;
        this.submissionQueueCurrentSize = data.submissionSize;

        // Update donut gauges
        this.repositoryQueueUsageData = [{ name: 'Repository Queue', value: (this.repositoryQueueCurrentSize / this.repositoryQueueCapacity) * 100 }];
        this.brokerQueueUsageData = [{ name: 'Broker Queue', value: (this.brokerQueueCurrentSize / this.brokerQueueCapacity) * 100 }];
        this.submissionQueueUsageData = [{ name: 'Submission Queue', value: (this.submissionQueueCurrentSize / this.submissionQueueCapacity) * 100 }];
      },
      error => console.error('Error fetching queue metrics:', error)
    );
  }

  fetchCpuMetrics() {
    this.http.get<any>(this.cpuMetricsApiUrl).subscribe(
      data => {
        const cpuUsage = data.cpuUsage;
        const cpuCount = data.cpuCount;
        const messagingCpuUsage = data.messagingCpuUsage ?? 0;
        const messagingCpuCount = data.messagingCpuCount ?? 0;

        this.updateCPUData(cpuUsage, cpuCount);
        this.updateMessagingCPUData(messagingCpuUsage, messagingCpuCount);
      },
      error => console.error('Error fetching CPU metrics:', error)
    );
  }

  fetchStartTime() {
    this.http.get<any>(this.startTimeApiUrl).subscribe(
      data => {
        const startTimestamp = data.measurements[0].value * 1000; // convert to ms
        this.serverStartTime = new Date(startTimestamp).toISOString();

        // Compute uptime in ms
        const now = Date.now();
        const uptimeMs = now - startTimestamp;
        this.serverUpTime = this.formatDuration(uptimeMs);
      },
      error => console.error('Error fetching start time:', error)
    );
  }

  fetchDatabaseMetrics() {
    this.http.get<any>(this.dbMetricsApiUrl).subscribe(
      data => {
        console.log('DB metrics:', data);
        // Add whatever handling you want for database metrics here
      },
      error => console.error('Error fetching database metrics:', error)
    );
  }

  updateHeapData(heapUtilizedMB: number, heapAllocatedMB: number) {

    const newSeriesAllocated = [
      ...this.heapMemoryData[0].series.slice(1).map((entry, index) => ({
        name: `${45 - (index) * this.updateIntervalS} s`,
        value: entry.value
      })),
      { name: '0 s', value: heapAllocatedMB }
    ];

    const newSeriesUtilized = [
      ...this.heapMemoryData[1].series.slice(1).map((entry, index) => ({
        name: `${45 - (index) * this.updateIntervalS} s`,
        value: entry.value
      })),
      { name: '0 s', value: heapUtilizedMB }
    ];

    this.heapMemoryData = [
      { name: 'Heap Allocated', series: newSeriesAllocated },
      { name: 'Heap Utilized', series: newSeriesUtilized }
    ];

    // Update Y-Axis max dynamically
    this.heapChartYScaleMax = Math.max(
      ...newSeriesUtilized.map(d => d.value),
      ...newSeriesAllocated.map(d => d.value)
    ) * (1 + this.yAxisBuffer);
  }

  updateCPUData(cpuUtilized: number, cpuCores: number) {
    this.totalCores = cpuCores;
    this.cpuUtilized = cpuUtilized;
    this.cpuUsagePercentage = cpuUtilized * 100; // Convert fraction to percentage

    // Update the chart data
    this.cpuUsageData = [
      {
        name: "CPU Usage",
        value: this.cpuUsagePercentage
      }
    ];
  }

  updateMessagingHeapData(heapUtilizedMB: number, heapAllocatedMB: number) {
    const newSeriesAllocated = [
      ...this.messagingHeapMemoryData[0].series.slice(1).map((entry, index) => ({
        name: `${45 - (index) * this.updateIntervalS} s`,
        value: entry.value
      })),
      { name: '0 s', value: heapAllocatedMB }
    ];

    const newSeriesUtilized = [
      ...this.messagingHeapMemoryData[1].series.slice(1).map((entry, index) => ({
        name: `${45 - (index) * this.updateIntervalS} s`,
        value: entry.value
      })),
      { name: '0 s', value: heapUtilizedMB }
    ];

    this.messagingHeapMemoryData = [
      { name: 'Messaging Heap Allocated', series: newSeriesAllocated },
      { name: 'Messaging Heap Utilized', series: newSeriesUtilized }
    ];

    this.messagingHeapChartYScaleMax = Math.max(
      ...newSeriesUtilized.map(d => d.value),
      ...newSeriesAllocated.map(d => d.value)
    ) * (1 + this.yAxisBuffer);
  }

  updateMessagingCPUData(cpuUtilized: number, cpuCores: number) {
    this.messagingCpuCores = cpuCores;
    this.messagingCpuUtilized = cpuUtilized;
    this.messagingCpuUsagePercentage = cpuUtilized * 100;

    this.messagingCpuUsageData = [
      {
        name: "Messaging CPU Usage",
        value: this.messagingCpuUsagePercentage
      }
    ];
  }

  updateTimeSeriesData<T extends { name: string; series: { name: string; value: number }[] }>(
    data: T[],
    newValue: number,
    label: string,
    updateIntervalS: number,
    yAxisBuffer: number
  ): { updatedData: T[]; newYScaleMax: number } {
    const updatedSeries = [
      ...data[0].series.slice(1).map((entry, index) => ({
        name: `${45 - index * updateIntervalS} s`,
        value: entry.value
      })),
      { name: '0 s', value: newValue }
    ];

    const newYScaleMax = Math.max(...updatedSeries.map(d => d.value)) * (1 + yAxisBuffer);

    return {
      updatedData: [{ name: label, series: updatedSeries }] as T[],
      newYScaleMax
    };
  }

  getRandomNumber(min: number, max: number): number {
      return Math.random() * (max - min) + min;
  }

  formatDuration(durationMs: number): string {
    const seconds = Math.floor(durationMs / 1000);
    const days = Math.floor(seconds / (3600 * 24));
    const hours = Math.floor((seconds % (3600 * 24)) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    const parts = [];
    if (days > 0) parts.push(`${days}d`);
    if (hours > 0 || days > 0) parts.push(`${hours}h`);
    if (minutes > 0 || hours > 0 || days > 0) parts.push(`${minutes}m`);
    parts.push(`${secs}s`);

    return parts.join(' ');
  }

  fetchDummyMetrics() {
    const heapUtilizedMB = this.getRandomNumber(100, 200);
    const heapAllocatedMB = this.getRandomNumber(300, 400);
    const cpuCores = 20;
    const cpuUtilized = Math.random();
    const writesPerSecond = this.getRandomNumber(100, 200);
    const readsPerSecond = this.getRandomNumber(100, 200);
    const bytesWrittenPerSecond = this.getRandomNumber(500, 1000);
    const bytesReadPerSecond = this.getRandomNumber(500, 1000);
    const totalConnections = this.getRandomNumber(0, 10);
    const messagingHeapUtilizedMB = this.getRandomNumber(100, 200);
    const messagingHeapAllocatedMB = this.getRandomNumber(300, 400);
    const messagingCpuCores = 10;
    const messagingCpuUtilized = Math.random();

    console.log('Dummy data:', {
      heapUtilizedMB, heapAllocatedMB, cpuUtilized, cpuCores,
      messagingHeapUtilizedMB, messagingHeapAllocatedMB, messagingCpuUtilized, messagingCpuCores
     });

    this.updateHeapData(heapUtilizedMB, heapAllocatedMB);
    this.updateCPUData(cpuUtilized, cpuCores);

    this.updateMessagingHeapData(messagingHeapUtilizedMB, messagingHeapAllocatedMB);
    this.updateMessagingCPUData(messagingCpuUtilized, messagingCpuCores);

    this.totalConnections = totalConnections;

    const resultWrites = this.updateTimeSeriesData(
      this.writesPerSecondData,
      writesPerSecond,
      'Writes Per Second',
      this.updateIntervalS,
      this.yAxisBuffer
    );
    this.writesPerSecondData = resultWrites.updatedData;
    this.writesYScaleMax = resultWrites.newYScaleMax;

    const resultReads = this.updateTimeSeriesData(
      this.readsPerSecondData,
      readsPerSecond,
      'Reads Per Second',
      this.updateIntervalS,
      this.yAxisBuffer
    );
    this.readsPerSecondData = resultReads.updatedData;
    this.readsYScaleMax = resultReads.newYScaleMax;

    const resultBytesWritten = this.updateTimeSeriesData(
      this.bytesWrittenPerSecondData,
      bytesWrittenPerSecond,
      'Bytes Written Per Second',
      this.updateIntervalS,
      this.yAxisBuffer
    );
    this.bytesWrittenPerSecondData = resultBytesWritten.updatedData;
    this.bytesWrittenYScaleMax = resultBytesWritten.newYScaleMax;

    const resultBytesRead = this.updateTimeSeriesData(
      this.bytesReadPerSecondData,
      bytesReadPerSecond,
      'Bytes Read Per Second',
      this.updateIntervalS,
      this.yAxisBuffer
    );
    this.bytesReadPerSecondData = resultBytesRead.updatedData;
    this.bytesReadYScaleMax = resultBytesRead.newYScaleMax;

    // Dummy gauge data
    this.repositoryQueueUsageData = [{ name: 'Repository Queue', value: this.getRandomNumber(10, 90) }];
    this.brokerQueueUsageData = [{ name: 'Broker Queue', value: this.getRandomNumber(10, 90) }];
    this.submissionQueueUsageData = [{ name: 'Submission Queue', value: this.getRandomNumber(10, 90) }];

    // Dummy server info
    this.takVersion = "Dummy-4.9.9";
    this.nodeId = "DUMMY-NODE-1234";

    this.cdr.detectChanges();
  }
}
