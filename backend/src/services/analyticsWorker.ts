import { Op } from 'sequelize';
import Interaction from '../models/Interaction';
import Metric from '../models/Metric';
import { sequelize } from '../config/database';

export class AnalyticsWorker {
  private static LATENCY_THRESHOLD = 5000; // 5 seconds
  private static ERROR_RATE_THRESHOLD = 0.05; // 5%

  static async aggregateMetrics() {
    try {
      const today = new Date().toISOString().split('T')[0];
      const startOfDay = new Date(today);
      const endOfDay = new Date(today);
      endOfDay.setDate(endOfDay.getDate() + 1);

      // Count scans (total interactions)
      const scans = await Interaction.count({
        where: {
          created_at: {
            [Op.gte]: startOfDay,
            [Op.lt]: endOfDay
          }
        }
      });

      // Count plays (completed interactions)
      const plays = await Interaction.count({
        where: {
          status: 'completed',
          created_at: {
            [Op.gte]: startOfDay,
            [Op.lt]: endOfDay
          }
        }
      });

      // Avg latency
      const latencyResult = await Interaction.findAll({
        attributes: [[sequelize.fn('avg', sequelize.col('latency_ms')), 'avgLatency']],
        where: {
          status: 'completed',
          created_at: {
            [Op.gte]: startOfDay,
            [Op.lt]: endOfDay
          }
        },
        raw: true
      });
      const avgLatency = parseFloat((latencyResult[0] as any).avgLatency || 0);

      // Likes/Dislikes
      const likes = await Interaction.count({
        where: {
          feedback: 'like',
          created_at: {
            [Op.gte]: startOfDay,
            [Op.lt]: endOfDay
          }
        }
      });

      const dislikes = await Interaction.count({
        where: {
          feedback: 'dislike',
          created_at: {
            [Op.gte]: startOfDay,
            [Op.lt]: endOfDay
          }
        }
      });

      // Upsert metric
      const [metric, created] = await Metric.findOrCreate({
        where: { date: today },
        defaults: {
          date: today,
          scans,
          plays,
          avg_latency_ms: avgLatency,
          likes,
          dislikes
        }
      });

      if (!created) {
        await metric.update({
          scans,
          plays,
          avg_latency_ms: avgLatency,
          likes,
          dislikes
        });
      }

      console.log(`[ANALYTICS] Aggregated metrics for ${today}`);

    } catch (error) {
      console.error("[ANALYTICS] Aggregation failed:", error);
    }
  }

  static async checkAlerts() {
    // Check last hour latency
    const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000);
    
    try {
      const recentInteractions = await Interaction.findAll({
        where: {
          created_at: { [Op.gte]: oneHourAgo },
          status: 'completed'
        }
      });

      if (recentInteractions.length === 0) return;

      const totalLatency = recentInteractions.reduce((sum, i) => sum + (i.latency_ms || 0), 0);
      const avgLatency = totalLatency / recentInteractions.length;

      if (avgLatency > this.LATENCY_THRESHOLD) {
        console.warn(`[ALERT] High latency detected: ${avgLatency}ms`);
        // In real app: send email/slack
      }

      // Error rate
      const errorCount = await Interaction.count({
        where: {
          created_at: { [Op.gte]: oneHourAgo },
          status: 'error'
        }
      });
      const totalCount = await Interaction.count({
        where: {
          created_at: { [Op.gte]: oneHourAgo }
        }
      });

      if (totalCount > 0 && (errorCount / totalCount) > this.ERROR_RATE_THRESHOLD) {
        console.warn(`[ALERT] High error rate detected: ${(errorCount / totalCount * 100).toFixed(2)}%`);
      }

    } catch (error) {
      console.error("[ANALYTICS] Alert check failed:", error);
    }
  }

  static start() {
    // Run every 5 minutes
    setInterval(() => {
      this.aggregateMetrics();
      this.checkAlerts();
    }, 5 * 60 * 1000);
    
    // Run immediately on start
    this.aggregateMetrics();
  }
}
