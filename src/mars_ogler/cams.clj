(ns mars-ogler.cams)

(def abbrev->cam {"ML" :mastcam
                  "MR" :mastcam
                  "NLA" :navcam
                  "NLB" :navcam
                  "NRA" :navcam
                  "NRB" :navcam
                  "CR" :chemcam
                  "CR0" :chemcam
                  "MH" :mahli
                  "MD" :mardi
                  "FLA" :hazcam
                  "FLB" :hazcam
                  "FRA" :hazcam
                  "FRB" :hazcam
                  "RLA" :hazcam
                  "RLB" :hazcam
                  "RRA" :hazcam
                  "RRB" :hazcam})

(def cam->name {:chemcam "ChemCam"
                :hazcam "HazCam"
                :mahli "MAHLI"
                :mardi "MARDI"
                :mastcam "MastCam"
                :navcam "NavCam"})

(def cams (keys cam->name))

(def abbrev->name {"ML" "MastCam Left"
                   "MR" "MastCam Right"
                   "NLA" "NavCam Left A"
                   "NLB" "NavCam Left B"
                   "NRA" "NavCam Right A"
                   "NRB" "NavCam Right B"
                   "CR" "ChemCam"
                   "CR0" "ChemCam"
                   "MH" "MAHLI"
                   "MD" "MARDI"
                   "FLA" "HazCam Front-Left A"
                   "FLB" "HazCam Front-Left B"
                   "FRA" "HazCam Front-Right A"
                   "FRB" "HazCam Front-Right B"
                   "RLA" "HazCam Rear-Left A"
                   "RLB" "HazCam Rear-Left B"
                   "RRA" "HazCam Rear-Right A"
                   "RRB" "HazCam Rear-Right B"})

(defn cam-parity
  [cam-name]
  (cond
    (re-find #"Left" cam-name) :left
    (re-find #"Right" cam-name) :right))
