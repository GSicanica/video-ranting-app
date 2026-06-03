import Foundation

func l10n(_ key: String, _ args: CVarArg... ) -> String {
    let format = NSLocalizedString(key, comment: "")
    if args.isEmpty { return format }
    return String(format: format, locale: Locale.current, arguments: args)
}
