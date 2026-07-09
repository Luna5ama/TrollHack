package dev.luna5ama.trollhack.config

/**
 * Structure:
 * ```
 * Categories
 *    â”œâ”€â–ºnamespace:default
 *    â”?    â”œâ”€â–ºNamespacedConfigurationManager(name1)
 *    â”?    â”?    â”œâ”€â–ºNamedConfigurables
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable1
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable2
 *    â”?    â”?    â”?    â””â”€â–?..
 *    â”?    â”?    â””â”€â–ºAnonymousConfigurables
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable1
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable2
 *    â”?    â”?          â””â”€â–?..
 *    â”?    â”œâ”€â–ºNamespacedConfigurationManager(name2)
 *    â”?    â”?    â”œâ”€â–ºNamedConfigurables
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable1
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable2
 *    â”?    â”?    â”?    â””â”€â–?..
 *    â”?    â”?    â””â”€â–ºAnonymousConfigurables
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable1
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable2
 *    â”?    â”?          â””â”€â–?..
 *    â”?    â””â”€â–?..
 *    â”‚â”€â–ºnamespace:preset1
 *    â”?    â”œâ”€â–ºNamespacedConfigurationManager(name1)
 *    â”?    â”?    â”œâ”€â–ºNamedConfigurables
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable1
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable2
 *    â”?    â”?    â”?    â””â”€â–?..
 *    â”?    â”?    â””â”€â–ºAnonymousConfigurables
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable1
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable2
 *    â”?    â”?          â””â”€â–?..
 *    â”?    â”œâ”€â–ºNamespacedConfigurationManager(name2)
 *    â”?    â”?    â”œâ”€â–ºNamedConfigurables
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable1
 *    â”?    â”?    â”?    â”œâ”€â–ºNamedConfigurable2
 *    â”?    â”?    â”?    â””â”€â–?..
 *    â”?    â”?    â””â”€â–ºAnonymousConfigurables
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable1
 *    â”?    â”?          â”œâ”€â–ºAnonymousConfigurable2
 *    â”?    â”?          â””â”€â–?..
 *    â”?    â””â”€â–?..
 *    â””â”€â–?..
 * ```
 */
class Categories {
    private val categoryMap = HashMap<String, NamespacedConfigurationManager>()

    fun getConfigurationManager(category: String) = categoryMap.computeIfAbsent(category) {
        NamespacedConfigurationManager(it)
    }

    fun clean() {
        categoryMap.values.forEach(NamespacedConfigurationManager::clean)
    }

    fun read() {
        categoryMap.values.forEach(NamespacedConfigurationManager::read)
    }

    fun save() {
        categoryMap.values.forEach(NamespacedConfigurationManager::save)
    }
}