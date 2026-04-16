#include <cstdio>
#include <cstring>
#include <cmath>
#include <vector>
#include <algorithm>
#include <random>

static const int IN = 2500;
static const int H1 = 256;
static const int H2 = 128;
static const int OUT = 10;

struct Sample { float x[IN]; int label;};

struct Net {
    std::vector<float> W1, b1; 
    std::vector<float> W2, b2;
    std::vector<float> W3, b3; 
};

static void mul(const float* W, const float* x, float* y, int rows, int cols) {
    memset(y, 0, cols * sizeof(float));
    for (int i = 0; i < rows; ++i) {
        float xi = x[i];
        for (int j = 0; j < cols; ++j) y[j] += W[i * cols + j] * xi;
    }
}

static void outerAdd(float* G, const float* a, const float* b, int rows, int cols) {
    for (int i = 0; i < rows; ++i)
        for (int j = 0; j < cols; ++j)
            G[i * cols + j] += a[i] * b[j];
}

static void relu(const float* z, float* a, int n) {
    for (int i = 0; i < n; ++i) a[i] = z[i] > 0 ? z[i] : 0;
}

static void softmax(float* v, int n) {
    float mx = *std::max_element(v, v + n);
    float sum = 0;
    for (int i = 0; i < n; ++i) { v[i] = std::exp(v[i] - mx); sum += v[i]; }
    for (int i = 0; i < n; ++i) v[i] /= sum;
}

static int argmax(const float* v, int n) {
    int best = 0;
    for (int i = 1; i < n; ++i) if (v[i] > v[best]) best = i;
    return best;
}

static void init(Net& net) {
    net.W1.resize(IN*H1); net.b1.assign(H1, 0);
    net.W2.resize(H1*H2); net.b2.assign(H2, 0);
    net.W3.resize(H2*OUT);net.b3.assign(OUT,0);

    std::mt19937 rng(42);
    auto fill = [&](std::vector<float>& W, int fan) {
        std::normal_distribution<float> d(0, std::sqrt(2.f / fan));
        for (auto& w : W) w = d(rng);
    };
    fill(net.W1, IN); fill(net.W2, H1); fill(net.W3, H2);
}

static std::vector<Sample> loadData(const char* path) {
    std::vector<Sample> samples;
    FILE* f = fopen(path, "r");
    if (!f) { fprintf(stderr, "Не удалось открыть %s\n", path); return samples; }

    char line[IN * 12];
    while (fgets(line, sizeof(line), f)) {
        char* pipe = strchr(line, '|');
        if (!pipe) continue;
        *pipe = '\0';
        int label = atoi(line);
        if (label < 0 || label > 9) continue;

        Sample s; s.label = label;
        char* tok = strtok(pipe + 1, ",\n\r");
        int idx = 0;
        while (tok && idx < IN) { s.x[idx++] = atof(tok); tok = strtok(nullptr, ",\n\r"); }
        if (idx == IN) samples.push_back(s);
    }
    fclose(f);
    return samples;
}

static void train(Net& net, std::vector<Sample>& data, int epochs, int batch, float lr) {
    Net g;
    g.W1.resize(net.W1.size()); g.b1.resize(H1);
    g.W2.resize(net.W2.size()); g.b2.resize(H2);
    g.W3.resize(net.W3.size()); g.b3.resize(OUT);

    std::mt19937 rng(0);
    int N = data.size();

    for (int ep = 0; ep < epochs; ++ep) {
        if (ep > 0 && ep % 10 == 0) lr *= 0.95f;
        std::shuffle(data.begin(), data.end(), rng);

        float loss = 0; int correct = 0;

        for (int bs = 0; bs < N; bs += batch) {
            int be = std::min(bs + batch, N);

            std::fill(g.W1.begin(),g.W1.end(),0); std::fill(g.b1.begin(),g.b1.end(),0);
            std::fill(g.W2.begin(),g.W2.end(),0); std::fill(g.b2.begin(),g.b2.end(),0);
            std::fill(g.W3.begin(),g.W3.end(),0); std::fill(g.b3.begin(),g.b3.end(),0);

            for (int si = bs; si < be; ++si) {
                const Sample& s = data[si];

                float z1[H1], a1[H1], z2[H2], a2[H2], z3[OUT];
                mul(net.W1.data(), s.x, z1, IN, H1);
                for (int j=0;j<H1;++j) z1[j]+=net.b1[j];
                relu(z1, a1, H1);

                mul(net.W2.data(), a1, z2, H1, H2);
                for (int j=0;j<H2;++j) z2[j]+=net.b2[j];
                relu(z2, a2, H2);

                mul(net.W3.data(), a2, z3, H2, OUT);
                for (int j=0;j<OUT;++j) z3[j]+=net.b3[j];
                softmax(z3, OUT);

                loss -= std::log(z3[s.label] + 1e-9f);
                if (argmax(z3, OUT) == s.label) ++correct;

                float d3[OUT], d2[H2], d1[H1];
                for (int j=0;j<OUT;++j) d3[j] = z3[j] - (j==s.label?1.f:0.f);

                outerAdd(g.W3.data(), a2, d3, H2, OUT);
                for (int j=0;j<OUT;++j) g.b3[j]+=d3[j];

                for (int i=0;i<H2;++i) {
                    float acc=0;
                    for (int j=0;j<OUT;++j) acc+=net.W3[i*OUT+j]*d3[j];
                    d2[i]=acc*(z2[i]>0?1.f:0.f);
                }
                outerAdd(g.W2.data(), a1, d2, H1, H2);
                for (int j=0;j<H2;++j) g.b2[j]+=d2[j];

                for (int i=0;i<H1;++i) {
                    float acc=0;
                    for (int j=0;j<H2;++j) acc+=net.W2[i*H2+j]*d2[j];
                    d1[i]=acc*(z1[i]>0?1.f:0.f);
                }
                outerAdd(g.W1.data(), s.x, d1, IN, H1);
                for (int j=0;j<H1;++j) g.b1[j]+=d1[j];
            }

            float sc = lr / (be - bs);
            for (size_t k=0;k<net.W1.size();++k) net.W1[k]-=sc*g.W1[k];
            for (size_t k=0;k<net.b1.size();++k) net.b1[k]-=sc*g.b1[k];
            for (size_t k=0;k<net.W2.size();++k) net.W2[k]-=sc*g.W2[k];
            for (size_t k=0;k<net.b2.size();++k) net.b2[k]-=sc*g.b2[k];
            for (size_t k=0;k<net.W3.size();++k) net.W3[k]-=sc*g.W3[k];
            for (size_t k=0;k<net.b3.size();++k) net.b3[k]-=sc*g.b3[k];
        }

        printf("Эпоха %2d/%d  loss=%.4f  acc=%.1f%%\n",
               ep+1, epochs, loss/N, 100.f*correct/N);
    }
}

static bool save(const Net& net, const char* path) {
    FILE* f = fopen(path, "wb");
    if (!f) return false;
    int32_t hdr[3] = {3, H1, H2};
    fwrite(hdr, 4, 3, f);
    fwrite(net.W1.data(), 4, IN*H1, f);
    fwrite(net.b1.data(), 4, H1, f);
    fwrite(net.W2.data(), 4, H1*H2, f);
    fwrite(net.b2.data(), 4, H2, f);
    fwrite(net.W3.data(), 4, H2*OUT, f);
    fwrite(net.b3.data(), 4, OUT, f);
    fclose(f);
    return true;
}

int main(int argc, char* argv[]) {
    const char* input  = argc > 1 ? argv[1] : "dataset_2px.txt";
    const char* output = argc > 2 ? argv[2] : "nn_weights.bin";

    printf("Загрузка данных из %s...\n", input);
    auto data = loadData(input);
    printf("Загружено %d образцов\n", (int)data.size());

    int cnt[10]={};
    for (auto& s : data) cnt[s.label]++;
    printf("По цифрам: ");
    for (int i=0;i<10;++i) printf("%d:%d ", i, cnt[i]);
    printf("\n");

    Net net; init(net);
    printf("Обучение...\n");
    train(net, data, 50, 32, 0.01f);

    if (save(net, output))
        printf("Веса сохранены в %s\n", output);

    return 0;
}
