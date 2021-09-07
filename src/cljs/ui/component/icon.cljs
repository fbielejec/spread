(ns ui.component.icon)

(def icons {:dropdown   "icons/icn_dropdown.svg"
            :run-new    "icons/icn_run_analysis.svg"
            :completed  "icons/icn_previous_analysis.svg"
            :kebab-menu "icons/icn_kebab menu.svg"
            :delete     "icons/icn_delete.svg"
            :user       "icons/icn_user.svg"
            :queue      "icons/icn_queue.svg"
            :upload     "icons/icn_upload.svg"
            :spread     "icons/icn_spread.svg"
            :google     "icons/icn_google.svg"
            :error      "icons/icn_error.svg"
            })

(defn arg->icon [icon]
  (if (string? icon)
    icon
    (icons icon)))
