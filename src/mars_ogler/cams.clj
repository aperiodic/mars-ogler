(ns mars-ogler.cams)

(def cams-by-abbrev {"ML" :mastcam
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

(def cams (-> (vals cams-by-abbrev) distinct set))

(def cam-names-by-cam {:chemcam "ChemCam"
                       :hazcam "HazCam"
                       :mahli "MAHLI"
                       :mardi "MARDI"
                       :mastcam "MastCam"
                       :navcam "NavCam"})

(def cam-names-by-abbrev {"ML" "MastCam Left"
                          "MR" "MastCam Right"
                          "NLA" "NavCam Left A"
                          "NLB" "NavCam Left B"
                          "NRA" "NavCam Right A"
                          "NRB" "NavCam Right B"
                          "CR" "ChemCam"
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
