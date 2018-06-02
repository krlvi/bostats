(ns bostats.db)

(def default-db
  {:stats          nil
   :selected-areas {:vasastan true :ostermalm true :kungsholmen true :sodermalm true :danderyd false :bergshamra false :norra-d true}
   :min-room       nil
   :max-room       nil
   :min-boarea     nil
   :max-boarea     nil
   :sold-age       3
   :resolution     "month"
   :loading        false
   :chart          nil})
