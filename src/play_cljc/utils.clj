(ns play-cljc.utils
  (:import [org.lwjgl.opengl GL41]))

(defn- create-shader [type source]
  (let [shader (GL41/glCreateShader type)]
    (GL41/glShaderSource shader source)
    (GL41/glCompileShader shader)
    (if (= GL41/GL_TRUE (GL41/glGetShaderi shader GL41/GL_COMPILE_STATUS))
      shader
      (throw (Exception. (GL41/glGetShaderInfoLog shader))))))

(defn create-program [_ v-source f-source]
  (let [vertex-shader (create-shader GL41/GL_VERTEX_SHADER v-source)
        fragment-shader (create-shader GL41/GL_FRAGMENT_SHADER f-source)
        program (GL41/glCreateProgram)]
    (GL41/glAttachShader program vertex-shader)
    (GL41/glAttachShader program fragment-shader)
    (GL41/glLinkProgram program)
    (if (= GL41/GL_TRUE (GL41/glGetProgrami program GL41/GL_LINK_STATUS))
      program
      (throw (Exception. (GL41/glGetProgramInfoLog program))))))

(defn create-buffer
  ([game program attrib-name src-data]
   (create-buffer game program attrib-name src-data {}))
  ([_ program attrib-name src-data
    {:keys [size type normalize stride offset]}]
   (let [attrib-location (GL41/glGetAttribLocation program attrib-name)
         buffer (GL41/glGenBuffers)]
     (GL41/glBindBuffer GL41/GL_ARRAY_BUFFER buffer)
     (GL41/glEnableVertexAttribArray attrib-location)
     (GL41/glVertexAttribPointer attrib-location size type normalize stride offset)
     (GL41/glBindBuffer GL41/GL_ARRAY_BUFFER buffer)
     (GL41/glBufferData GL41/GL_ARRAY_BUFFER src-data GL41/GL_STATIC_DRAW)
     (/ (.-length src-data) size))))

(defn create-index-buffer [_ indices]
  (let [index-buffer (GL41/glGenBuffers)]
    (GL41/glBindBuffer GL41/GL_ELEMENT_ARRAY_BUFFER index-buffer)
    (GL41/glBufferData GL41/GL_ELEMENT_ARRAY_BUFFER indices GL41/GL_STATIC_DRAW)
    (.-length indices)))

(defn get-width [_]
  0)

(defn get-height [_]
  0)

(defmacro get-enum [_ k]
  (symbol (str "GL41/GL_" (name k))))

(defmacro enable [_ k]
  (list 'GL41/glEnable (symbol (str "GL41/GL_" (name k)))))

