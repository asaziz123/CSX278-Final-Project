(ns ^:figwheel-no-load engn-web.dev
  (:require
    [engn-web.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
