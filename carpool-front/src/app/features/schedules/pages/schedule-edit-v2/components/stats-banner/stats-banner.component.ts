import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Stats } from '../../../../../../modules/openapi';

@Component({
  selector: 'app-stats-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats-banner.component.html',
  styleUrl: './stats-banner.component.css'
})
export class StatsBannerComponent implements OnInit {
  @Input() stats: Stats[] = [];
  @Input() show: boolean = false;

  totalTrips: number = 0;
  averageLoad: number = 0;
  mostActiveFamily: Stats | null = null;
  leastActiveFamily: Stats | null = null;
  balanceScore: number = 0;

  ngOnInit(): void {
    this.calculateGlobalStats();
  }

  ngOnChanges(): void {
    this.calculateGlobalStats();
  }

  private calculateGlobalStats(): void {
    if (!this.stats || this.stats.length === 0) return;

    // Total des trajets
    this.totalTrips = this.stats.reduce((sum, stat) => sum + (stat.meanTripPerWeek || 0), 0) * 2; // x2 car c'est par semaine

    // Charge moyenne
    this.averageLoad = this.totalTrips / this.stats.length;

    // Famille la plus active
    this.mostActiveFamily = this.stats.reduce((max, stat) => 
      (stat.meanTripPerWeek || 0) > (max.meanTripPerWeek || 0) ? stat : max
    );

    // Famille la moins active
    this.leastActiveFamily = this.stats.reduce((min, stat) => 
      (stat.meanTripPerWeek || 0) < (min.meanTripPerWeek || 0) ? stat : min
    );

    // Score d'équilibre (0-100, 100 = parfaitement équilibré)
    const deviations = this.stats.map(stat => {
      const actual = stat.meanTripPerWeek || 0;
      const perfect = stat.perfectMeanTripPerWeek || 0;
      return perfect > 0 ? Math.abs(actual - perfect) / perfect : 0;
    });
    
    const averageDeviation = deviations.reduce((sum, dev) => sum + dev, 0) / deviations.length;
    this.balanceScore = Math.max(0, Math.min(100, (1 - averageDeviation) * 100));
  }

  getBalanceColor(): string {
    if (this.balanceScore >= 80) return '#10b981'; // Vert
    if (this.balanceScore >= 60) return '#f59e0b'; // Orange
    return '#ef4444'; // Rouge
  }

  getBalanceLabel(): string {
    if (this.balanceScore >= 80) return 'Excellent équilibre';
    if (this.balanceScore >= 60) return 'Équilibre correct';
    return 'Déséquilibre détecté';
  }

  getBalanceIcon(): string {
    if (this.balanceScore >= 80) return '🎯';
    if (this.balanceScore >= 60) return '⚖️';
    return '⚠️';
  }

  /**
   * Calcule le pourcentage de charge par rapport à l'idéal pour une famille
   */
  getLoadPercentage(stat: Stats): number {
    const actual = stat.meanTripPerWeek || 0;
    const perfect = stat.perfectMeanTripPerWeek || 0;
    return perfect > 0 ? (actual / perfect) * 100 : 0;
  }

  /**
   * Détermine la couleur de la barre de charge
   */
  getLoadColor(stat: Stats): string {
    const percentage = this.getLoadPercentage(stat);
    if (percentage >= 90 && percentage <= 110) return '#10b981'; // Vert - équilibré
    if (percentage >= 70 && percentage <= 130) return '#f59e0b'; // Orange - acceptable
    return '#ef4444'; // Rouge - déséquilibré
  }

  /**
   * Détermine le statut de la charge
   */
  getLoadStatus(stat: Stats): string {
    const percentage = this.getLoadPercentage(stat);
    if (percentage >= 90 && percentage <= 110) return 'Équilibré';
    if (percentage < 90) return 'Sous-chargé';
    return 'Surchargé';
  }

  /**
   * Trie les stats par charge décroissante
   */
  getSortedStats(): Stats[] {
    return [...this.stats].sort((a, b) => (b.meanTripPerWeek || 0) - (a.meanTripPerWeek || 0));
  }
}