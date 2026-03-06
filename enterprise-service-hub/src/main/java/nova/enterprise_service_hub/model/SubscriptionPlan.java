package nova.enterprise_service_hub.model;

/**
 * Subscription tiers available on the platform.
 * Each tier unlocks a progressively larger set of modules.
 *
 * <ul>
 *   <li><b>FREE</b>      — Basic CMS only (services, projects, pages)</li>
 *   <li><b>STARTER</b>   — + Finance module</li>
 *   <li><b>PROFESSIONAL</b> — + AI Content Engine, Advanced Analytics</li>
 *   <li><b>ELITE</b>     — All modules unlocked</li>
 * </ul>
 */
public enum SubscriptionPlan {
    FREE,
    STARTER,
    PROFESSIONAL,
    ELITE
}
