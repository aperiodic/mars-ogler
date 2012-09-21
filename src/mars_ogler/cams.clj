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

(def cam-names-by-abbrev {"ML" "MastCam Left"
                          "MR" "MastCam Right"
                          "NLA" "NavCam Left"
                          "NLB" "NavCam Left"
                          "NRA" "NavCam Right"
                          "NRB" "NavCam Right"
                          "CR" "ChemCam"
                          "MH" "MAHLI"
                          "MD" "MARDI"
                          "FLA" "HazCam Front-Left"
                          "FLB" "HazCam Front-Left"
                          "FRA" "HazCam Front-Right"
                          "FRB" "HazCam Front-Right"
                          "RLA" "HazCam Rear-Left"
                          "RLB" "HazCam Rear-Left"
                          "RRA" "HazCam Rear-Right"
                          "RRB" "HazCam Rear-Right"})
