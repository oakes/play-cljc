(ns play-cljc.utils
  (:import [org.lwjgl.opengl GL GL30]))

(defn- create-shader [type source]
  (let [shader (GL30/glCreateShader type)]
    (GL30/glShaderSource shader source)
    (GL30/glCompileShader shader)
    (if (= GL30/GL_TRUE (GL30/glGetShaderi shader GL30/GL_COMPILE_STATUS))
      shader
      (throw (Exception. (GL30/glGetShaderInfoLog shader))))))

(defn create-program [_ v-source f-source]
  (let [vertex-shader (create-shader GL30/GL_VERTEX_SHADER v-source)
        fragment-shader (create-shader GL30/GL_FRAGMENT_SHADER f-source)
        program (GL30/glCreateProgram)]
    (GL30/glAttachShader program vertex-shader)
    (GL30/glAttachShader program fragment-shader)
    (GL30/glLinkProgram program)
    (if (= GL30/GL_TRUE (GL30/glGetProgrami program GL30/GL_LINK_STATUS))
      program
      (throw (Exception. (GL30/glGetProgramInfoLog program))))))

(defn create-buffer
  ([game program attrib-name src-data]
   (create-buffer game program attrib-name src-data {}))
  ([_ program attrib-name src-data
    {:keys [size type normalize stride offset]}]
   (let [attrib-location (GL30/glGetAttribLocation program attrib-name)
         buffer (GL30/glGenBuffers)]
     (GL30/glBindBuffer GL30/GL_ARRAY_BUFFER buffer)
     (GL30/glEnableVertexAttribArray attrib-location)
     (GL30/glVertexAttribPointer attrib-location size type normalize stride offset)
     (GL30/glBindBuffer GL30/GL_ARRAY_BUFFER buffer)
     (GL30/glBufferData GL30/GL_ARRAY_BUFFER src-data GL30/GL_STATIC_DRAW)
     (/ (.-length src-data) size))))

(defn create-index-buffer [_ indices]
  (let [index-buffer (GL30/glGenBuffers)]
    (GL30/glBindBuffer GL30/GL_ELEMENT_ARRAY_BUFFER index-buffer)
    (GL30/glBufferData GL30/GL_ELEMENT_ARRAY_BUFFER indices GL30/GL_STATIC_DRAW)
    (.-length indices)))

(defn get-width [_]
  0)

(defn get-height [_]
  0)

(defmacro get-enum [_ k]
  (symbol (str "GL30/GL_" (name k))))

(defmacro enable [_ k]
  (list 'GL30/glEnable (symbol (str "GL30/GL_" (name k)))))

