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
  }

  totalWrites = -1;
  totalReads = -1;
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

    // Fetch metrics immediately and then every 5 seconds
    this.fetchMetrics();
    console.log('Setting up periodic metrics fetching');
    this.updateInterval = setInterval(() => this.fetchMetrics(), this.updateIntervalMs);
  }

  ngOnDestroy() {
    // Clear the interval when the component is destroyed to prevent memory leaks
    if (this.updateInterval) {
      clearInterval(this.updateInterval);
    }
  }

  fetchMetrics() {
    if (this.useDummyData) {
      // Generate dummy heap values
      const heapUtilizedMB = this.getRandomNumber(100, 200);
      const heapAllocatedMB = this.getRandomNumber(300, 400);
      const cpuCores = 20;
      const cpuUtilized = Math.random();
      const writesPerSecond = this.getRandomNumber(100, 200);
      const readsPerSecond = this.getRandomNumber(100, 200);
      const bytesWrittenPerSecond = this.getRandomNumber(500, 1000);
      const bytesReadPerSecond = this.getRandomNumber(500, 1000);
      const totalConnections = this.getRandomNumber(0, 10);

      console.log('Using dummy data:', { heapUtilizedMB, heapAllocatedMB, cpuUtilized, cpuCores });

      this.updateHeapData(heapUtilizedMB, heapAllocatedMB);
      this.updateCPUData(cpuUtilized, cpuCores);

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

      this.totalWrites += this.getRandomNumber(0, 20);
      this.totalReads += this.getRandomNumber(0, 20);

    } else {
      this.http.get<any>(this.globalMetricsApiUrl).subscribe(
        (data) => {
          const heapUtilizedMB = data.heapUtilized / 1048576;
          const heapAllocatedMB = data.heapAllocated / 1048576;
          const totalConnectedClients = data.numConnectedClients;
          const writesPerSecond = data.writesPerSecond;
          const readsPerSecond = data.readsPerSecond;
          const bytesWrittenPerSecond = data.bytesWrittenPerSecond;
          const bytesReadPerSecond = data.bytesReadPerSecond;
          console.log('Received global API data:', { heapUtilizedMB, heapAllocatedMB, totalConnectedClients, writesPerSecond, readsPerSecond, bytesWrittenPerSecond, bytesReadPerSecond });

          this.updateHeapData(heapUtilizedMB, heapAllocatedMB);
          this.updateCPUData(data.cpuUtilized, data.cpuCores);

          this.totalConnections = totalConnectedClients;

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
        },
        (error) => console.error('Error fetching global metrics:', error)
      );

      this.http.get<any>(this.metricsApiUrl).subscribe(
        (data) => {
          const totalWrites = data.totalWrites;
          const totalReads = data.totalReads;
          console.log('Received API data:', { totalWrites, totalReads });

          this.totalWrites = totalWrites;
          this.totalReads = totalReads;

        },
        (error) => console.error('Error fetching metrics:', error)
      );
    }
    this.cdr.detectChanges();
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
}
