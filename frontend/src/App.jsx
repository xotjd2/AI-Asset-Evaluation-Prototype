import { useEffect, useState } from "react";

const API_BASE = `${import.meta.env.VITE_API_BASE_URL || ""}/api/evaluations`;

const initialStockForm = {
  stockName: "삼성전자",
  currentPrice: 82000,
  expectedAnnualReturn: 0.12,
  volatility: 0.24,
  holdingDays: 90,
  shares: 1200,
};

const initialBondForm = {
  bondName: "국고채 5년",
  faceValue: 100000000,
  couponRate: 0.045,
  marketYield: 0.038,
  maturityYears: 5,
  confidenceLevel: 0.95,
};

const initialProjectForm = {
  projectName: "물류센터 개발사업",
  initialInvestment: 5000000000,
  discountRate: 0.09,
  probabilityOfDefault: 0.06,
  cashFlows: [1200000000, 1400000000, 1600000000, 1800000000, 1900000000],
};

const metricDescriptions = {
  presentValue: "현재 기준으로 계산한 평가 금액입니다.",
  riskMetricValue: "상품별 핵심 위험지표입니다. 주식은 VaR, 채권은 금리충격손실, 프로젝트는 신용위험추정치입니다.",
  duration: "금리 변화에 대한 가격 민감도를 나타냅니다.",
  convexity: "금리 변화에 따른 곡선 효과를 보정하는 지표입니다.",
  npv: "미래 현금흐름의 현재가치 합에서 초기 투자비를 뺀 값입니다.",
  irr: "순현재가치를 0으로 만드는 내부수익률입니다.",
  paybackPeriod: "초기 투자금을 회수하는 데 걸리는 기간입니다.",
  riskScore: "내부 규칙 기반 종합 위험 점수입니다.",
};

function toNumber(value) {
  if (typeof value === "number") return value;
  const normalized = String(value).replace(/,/g, "").trim();
  return normalized === "" ? 0 : Number(normalized);
}

function formatMoney(value) {
  const numeric = toNumber(value);
  return Number.isNaN(numeric) ? "" : numeric.toLocaleString("ko-KR");
}

function formatMoneyLabel(value) {
  const numeric = toNumber(value);
  if (!numeric) return "0원";
  if (Math.abs(numeric) >= 100000000) {
    return `${(numeric / 100000000).toFixed(1)}억 원`;
  }
  return `${numeric.toLocaleString("ko-KR")}원`;
}

function formatMetric(value, isMoney = false) {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  if (isMoney) return `${value.toLocaleString("ko-KR")}원`;
  return value.toLocaleString("ko-KR", { maximumFractionDigits: 4 });
}

function adjustByPercent(value, direction, minimum = 0) {
  const numeric = toNumber(value);
  const adjusted = Math.round(numeric * (1 + 0.01 * direction));
  return Math.max(adjusted, minimum);
}

function stockScenarios(form) {
  const principal = toNumber(form.currentPrice) * toNumber(form.shares);
  const horizon = toNumber(form.holdingDays) / 252;
  const calc = (ret, vol) => ({
    expectedValue: principal * Math.pow(1 + ret, horizon),
    riskMetricValue: principal * vol * Math.sqrt(horizon) * 1.65,
  });

  const base = calc(Number(form.expectedAnnualReturn), Number(form.volatility));
  const upside = calc(Number(form.expectedAnnualReturn) + 0.05, Number(form.volatility));
  const downside = calc(Math.max(Number(form.expectedAnnualReturn) - 0.05, -0.95), Number(form.volatility) + 0.05);

  return [
    { name: "기준", ...base },
    { name: "수익률 +5%p", ...upside },
    { name: "수익률 -5%p / 변동성 +5%p", ...downside },
  ];
}

function bondScenarios(form, result) {
  const baseYield = Number(form.marketYield);
  const price = result.presentValue;
  const duration = result.duration;
  const convexity = result.convexity;

  const estimatePrice = (deltaYield) =>
    price * (1 - duration * deltaYield + 0.5 * convexity * deltaYield * deltaYield);

  return [
    { name: "기준", yieldValue: baseYield, estimatedPrice: price },
    { name: "금리 +50bp", yieldValue: baseYield + 0.005, estimatedPrice: estimatePrice(0.005) },
    { name: "금리 +100bp", yieldValue: baseYield + 0.01, estimatedPrice: estimatePrice(0.01) },
    { name: "금리 -50bp", yieldValue: baseYield - 0.005, estimatedPrice: estimatePrice(-0.005) },
  ];
}

function projectNpv(initialInvestment, discountRate, cashFlows) {
  let npv = -initialInvestment;
  cashFlows.forEach((cashFlow, index) => {
    npv += cashFlow / Math.pow(1 + discountRate, index + 1);
  });
  return npv;
}

function projectScenarios(form) {
  const initialInvestment = toNumber(form.initialInvestment);
  const discountRate = Number(form.discountRate);
  const probabilityOfDefault = Number(form.probabilityOfDefault);
  const cashFlows = form.cashFlows.map((value) => toNumber(value));

  const calc = (rate, pd) => {
    const npv = projectNpv(initialInvestment, rate, cashFlows);
    const presentValue = npv + initialInvestment;
    const creditRiskEstimate = Math.max(presentValue, 0) * pd * 0.45;
    return { npv, creditRiskEstimate, rate, pd };
  };

  return [
    { name: "기준", ...calc(discountRate, probabilityOfDefault) },
    { name: "할인율 +1%p", ...calc(discountRate + 0.01, probabilityOfDefault) },
    { name: "할인율 -1%p", ...calc(Math.max(discountRate - 0.01, 0.0001), probabilityOfDefault) },
    { name: "부도확률 +2%p", ...calc(discountRate, Math.min(probabilityOfDefault + 0.02, 1)) },
  ];
}

function MetricCard({ item }) {
  return (
    <div className="metric-card">
      <div className="metric-label-row">
        <span>{item.label}</span>
        <span className="info-chip">
          i
          <span className="tooltip-panel">{metricDescriptions[item.key] || `${item.label} 설명`}</span>
        </span>
      </div>
      <strong>{formatMetric(item.value, item.isMoney)}</strong>
      {item.isMoney ? <small>{formatMoneyLabel(item.value)}</small> : null}
    </div>
  );
}

function MoneyField({ label, value, onChange, onAdjust, wide = false }) {
  return (
    <label className={`field money-field ${wide ? "field-span-2" : ""}`}>
      <span>{label}</span>
      <div className="money-input-shell">
        <input type="text" inputMode="numeric" value={formatMoney(value)} onChange={(e) => onChange(toNumber(e.target.value))} />
        <div className="money-actions always-visible">
          <button type="button" className="ghost-button subtle" onClick={() => onAdjust(-1)}>-1%</button>
          <button type="button" className="ghost-button subtle" onClick={() => onAdjust(1)}>+1%</button>
        </div>
      </div>
      <small>{formatMoneyLabel(value)}</small>
    </label>
  );
}

function NumberField({ label, value, onChange, step = "1" }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type="number" step={step} value={value} onChange={(e) => onChange(e.target.value)} />
    </label>
  );
}

function CashFlowList({ cashFlows, onChange }) {
  return (
    <div className="cashflow-section field-span-2">
      <div className="cashflow-header">
        <div>
          <span className="cashflow-title">연도별 현금흐름</span>
          <p>각 연차에 유입될 것으로 가정한 현금흐름입니다.</p>
        </div>
      </div>
      <div className="cashflow-list">
        {cashFlows.map((value, index) => (
          <label className="cashflow-row" key={`year-${index + 1}`}>
            <span className="cashflow-year">{index + 1}년차</span>
            <div className="cashflow-input-wrap">
              <input
                type="text"
                inputMode="numeric"
                value={formatMoney(value)}
                onChange={(e) => {
                  const next = [...cashFlows];
                  next[index] = toNumber(e.target.value);
                  onChange(next);
                }}
              />
              <small>{formatMoneyLabel(value)}</small>
            </div>
          </label>
        ))}
      </div>
    </div>
  );
}

function buildMetricItems(result) {
  const base = [
    { key: "presentValue", label: "평가금액", value: result.presentValue, isMoney: true },
    { key: "riskMetricValue", label: result.riskMetricName, value: result.riskMetricValue, isMoney: true },
    { key: "riskScore", label: "위험점수", value: result.riskScore },
  ];

  if (result.evaluationType === "주식") return base;
  if (result.evaluationType === "채권") {
    return [...base, { key: "duration", label: "듀레이션", value: result.duration }, { key: "convexity", label: "컨벡서티", value: result.convexity }];
  }
  if (result.evaluationType === "프로젝트") {
    return [...base, { key: "npv", label: "NPV", value: result.npv, isMoney: true }, { key: "irr", label: "IRR", value: result.irr }, { key: "paybackPeriod", label: "회수기간", value: result.paybackPeriod }];
  }
  return base;
}

function SensitivityPanel({ result, inputs }) {
  const scenarios =
    result.evaluationType === "주식"
      ? stockScenarios(inputs)
      : result.evaluationType === "채권"
        ? bondScenarios(inputs, result)
        : projectScenarios(inputs);

  return (
    <div className="detail-panel">
      <h4>민감도 분석</h4>
      <p className="detail-copy">기준값에서 주요 가정을 조금씩 바꿨을 때 결과가 얼마나 변하는지 간단히 확인합니다.</p>
      <div className="scenario-list">
        {scenarios.map((scenario) => (
          <div className="scenario-card" key={scenario.name}>
            <strong>{scenario.name}</strong>
            {result.evaluationType === "주식" ? (
              <>
                <span>기대가치: {formatMetric(scenario.expectedValue, true)}</span>
                <span>VaR 95%: {formatMetric(scenario.riskMetricValue, true)}</span>
              </>
            ) : null}
            {result.evaluationType === "채권" ? (
              <>
                <span>시장수익률: {(scenario.yieldValue * 100).toFixed(2)}%</span>
                <span>추정 채권가치: {formatMetric(scenario.estimatedPrice, true)}</span>
              </>
            ) : null}
            {result.evaluationType === "프로젝트" ? (
              <>
                <span>할인율: {(scenario.rate * 100).toFixed(2)}%</span>
                <span>NPV: {formatMetric(scenario.npv, true)}</span>
                <span>신용위험추정치: {formatMetric(scenario.creditRiskEstimate, true)}</span>
              </>
            ) : null}
          </div>
        ))}
      </div>
    </div>
  );
}

function InputSummary({ result, inputs }) {
  return (
    <div className="detail-panel">
      <h4>입력값 요약</h4>
      <div className="input-summary">
        {result.evaluationType === "주식" ? (
          <>
            <span>현재 주가: {formatMoneyLabel(inputs.currentPrice)}</span>
            <span>기대 연수익률: {(Number(inputs.expectedAnnualReturn) * 100).toFixed(2)}%</span>
            <span>변동성: {(Number(inputs.volatility) * 100).toFixed(2)}%</span>
            <span>보유일수: {inputs.holdingDays}일</span>
          </>
        ) : null}
        {result.evaluationType === "채권" ? (
          <>
            <span>액면가: {formatMoneyLabel(inputs.faceValue)}</span>
            <span>쿠폰금리: {(Number(inputs.couponRate) * 100).toFixed(2)}%</span>
            <span>시장수익률: {(Number(inputs.marketYield) * 100).toFixed(2)}%</span>
            <span>만기: {inputs.maturityYears}년</span>
          </>
        ) : null}
        {result.evaluationType === "프로젝트" ? (
          <>
            <span>초기 투자비: {formatMoneyLabel(inputs.initialInvestment)}</span>
            <span>할인율: {(Number(inputs.discountRate) * 100).toFixed(2)}%</span>
            <span>부도확률: {(Number(inputs.probabilityOfDefault) * 100).toFixed(2)}%</span>
            <span>현금흐름 개수: {inputs.cashFlows.length}개</span>
          </>
        ) : null}
      </div>
    </div>
  );
}

function ResultPanel({ title, result, inputs, onSave, onToggleDetail, detailOpen, saving, saved }) {
  if (!result) {
    return (
      <section className="result-panel empty">
        <h3>{title}</h3>
        <p>평가 실행 후 결과와 DB 저장 버튼이 표시됩니다.</p>
      </section>
    );
  }

  return (
    <section className="result-panel">
      <div className="result-header">
        <div>
          <p className="eyebrow">{result.evaluationType}</p>
          <h3>{result.targetName}</h3>
        </div>
        <div className={`grade grade-${result.riskGrade}`}>{result.riskGrade}</div>
      </div>

      <div className="metric-grid compact-grid">
        {buildMetricItems(result).map((item) => (
          <MetricCard key={`${item.key}-${item.label}`} item={item} />
        ))}
      </div>

      <div className="explanation-block">
        <h4>계산 설명</h4>
        <p>{result.modelExplanation}</p>
      </div>

      <div className="commentary-block">
        <h4>종합 의견</h4>
        <p>{result.commentary}</p>
      </div>

      {detailOpen ? (
        <div className="detail-stack">
          <InputSummary result={result} inputs={inputs} />
          <SensitivityPanel result={result} inputs={inputs} />
        </div>
      ) : null}

      <ul className="highlight-list">
        {result.highlights?.map((item) => (
          <li key={item}>{item}</li>
        ))}
      </ul>

      <div className="result-actions-row">
        <button className="secondary-button" type="button" onClick={onToggleDetail}>
          {detailOpen ? "상세 닫기" : "상세 보기"}
        </button>
        <button className="primary-button action-button" type="button" onClick={onSave} disabled={saving || saved}>
          {saved ? "DB 저장 완료" : saving ? "저장 중..." : "DB 저장"}
        </button>
      </div>
    </section>
  );
}

function RecentSection({ items }) {
  const grouped = ["주식", "채권", "프로젝트"].map((type) => ({
    type,
    items: items.filter((item) => item.evaluationType === type),
  }));

  return (
    <section className="recent-section">
      <div className="section-heading">
        <h2>최근 저장 이력</h2>
      </div>
      <div className="recent-type-grid">
        {grouped.map((group) => (
          <div className="recent-column" key={group.type}>
            <div className="recent-column-header">
              <h3>{group.type}</h3>
            </div>
            {group.items.length === 0 ? (
              <div className="recent-empty">저장된 {group.type} 평가가 없습니다.</div>
            ) : (
              group.items.map((item, index) => (
                <article className="recent-card" key={`${group.type}-${item.targetName}-${index}`}>
                  <div className="recent-top">
                    <span>{item.evaluationType}</span>
                    <strong>{item.riskGrade}</strong>
                  </div>
                  <h3>{item.targetName}</h3>
                  <p>{item.commentary}</p>
                  <small>{item.evaluatedAt ? new Date(item.evaluatedAt).toLocaleString() : ""}</small>
                </article>
              ))
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

export default function App() {
  const [stockForm, setStockForm] = useState(initialStockForm);
  const [bondForm, setBondForm] = useState(initialBondForm);
  const [projectForm, setProjectForm] = useState(initialProjectForm);
  const [stockResult, setStockResult] = useState(null);
  const [bondResult, setBondResult] = useState(null);
  const [projectResult, setProjectResult] = useState(null);
  const [detailOpen, setDetailOpen] = useState({ stock: false, bond: false, project: false });
  const [savedState, setSavedState] = useState({ stock: false, bond: false, project: false });
  const [savingState, setSavingState] = useState({ stock: false, bond: false, project: false });
  const [recentItems, setRecentItems] = useState([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const fetchRecent = async () => {
    try {
      const response = await fetch(`${API_BASE}/recent`);
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "최근 이력을 가져오지 못했습니다.");
      setRecentItems(data);
    } catch {
      setRecentItems([]);
    }
  };

  useEffect(() => {
    fetchRecent();
  }, []);

  const evaluate = async (path, payload, setter, key, fallbackMessage) => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(`${API_BASE}/${path}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || fallbackMessage);
      setter(data);
      setSavedState((prev) => ({ ...prev, [key]: false }));
      setDetailOpen((prev) => ({ ...prev, [key]: false }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const saveResult = async (key, result) => {
    if (!result) return;
    setSavingState((prev) => ({ ...prev, [key]: true }));
    setError("");
    try {
      const response = await fetch(`${API_BASE}/save`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(result),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || "DB 저장에 실패했습니다.");
      setSavedState((prev) => ({ ...prev, [key]: true }));
      fetchRecent();
      if (key === "stock") setStockResult(data);
      if (key === "bond") setBondResult(data);
      if (key === "project") setProjectResult(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingState((prev) => ({ ...prev, [key]: false }));
    }
  };

  const toggleDetail = (key) => {
    setDetailOpen((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="page-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">AI Asset Evaluation Prototype</p>
          <h1>금융상품 및 프로젝트 평가 시스템</h1>
          <p className="hero-copy">
            각 금융상품의 특성에 맞는 위험지표를 개별적으로 적용하였으며,<br />
            절대적인 비교가 아닌 "상품별 리스크 판단"을 목적으로 설계되었습니다.<br />
            상세보기를 열면 입력값 요약과 민감도 분석을 함께 확인할 수 있습니다.
          </p>
        </div>
        <div className="hero-panel">
          <span>워크플로우</span>
          <strong>입력 → 평가 실행 → 상세 분석 → DB 저장</strong>
        </div>
      </header>

      {error ? <div className="error-banner">{error}</div> : null}

      <section className="form-grid">
        <form className="panel" onSubmit={(event) => {
          event.preventDefault();
          evaluate(
            "stock",
            {
              ...stockForm,
              currentPrice: toNumber(stockForm.currentPrice),
              expectedAnnualReturn: Number(stockForm.expectedAnnualReturn),
              volatility: Number(stockForm.volatility),
              holdingDays: Number(stockForm.holdingDays),
              shares: Number(stockForm.shares),
            },
            setStockResult,
            "stock",
            "주식 평가에 실패했습니다."
          );
        }}>
          <div className="panel-title">
            <h2>주식 평가</h2>
            <span>수익률·변동성 기준</span>
          </div>
          <div className="field-grid">
            <label className="field">
              <span>종목명</span>
              <input value={stockForm.stockName} onChange={(e) => setStockForm({ ...stockForm, stockName: e.target.value })} />
            </label>
            <MoneyField label="현재 주가" value={stockForm.currentPrice} wide onChange={(value) => setStockForm({ ...stockForm, currentPrice: value })} onAdjust={(direction) => setStockForm({ ...stockForm, currentPrice: adjustByPercent(stockForm.currentPrice, direction, 1) })} />
            <NumberField label="기대 연수익률" step="0.001" value={stockForm.expectedAnnualReturn} onChange={(value) => setStockForm({ ...stockForm, expectedAnnualReturn: value })} />
            <NumberField label="변동성" step="0.001" value={stockForm.volatility} onChange={(value) => setStockForm({ ...stockForm, volatility: value })} />
            <NumberField label="보유일수" value={stockForm.holdingDays} onChange={(value) => setStockForm({ ...stockForm, holdingDays: value })} />
            <NumberField label="수량" value={stockForm.shares} onChange={(value) => setStockForm({ ...stockForm, shares: value })} />
          </div>
          <button className="primary-button" type="submit" disabled={loading}>주식 평가 실행</button>
        </form>

        <form className="panel" onSubmit={(event) => {
          event.preventDefault();
          evaluate(
            "bond",
            {
              ...bondForm,
              faceValue: toNumber(bondForm.faceValue),
              couponRate: Number(bondForm.couponRate),
              marketYield: Number(bondForm.marketYield),
              maturityYears: Number(bondForm.maturityYears),
              confidenceLevel: Number(bondForm.confidenceLevel),
            },
            setBondResult,
            "bond",
            "채권 평가에 실패했습니다."
          );
        }}>
          <div className="panel-title">
            <h2>채권 평가</h2>
            <span>금리 민감도 기준</span>
          </div>
          <div className="field-grid">
            <label className="field">
              <span>채권명</span>
              <input value={bondForm.bondName} onChange={(e) => setBondForm({ ...bondForm, bondName: e.target.value })} />
            </label>
            <MoneyField label="액면가" value={bondForm.faceValue} wide onChange={(value) => setBondForm({ ...bondForm, faceValue: value })} onAdjust={(direction) => setBondForm({ ...bondForm, faceValue: adjustByPercent(bondForm.faceValue, direction, 1) })} />
            <NumberField label="쿠폰금리" step="0.001" value={bondForm.couponRate} onChange={(value) => setBondForm({ ...bondForm, couponRate: value })} />
            <NumberField label="시장수익률" step="0.001" value={bondForm.marketYield} onChange={(value) => setBondForm({ ...bondForm, marketYield: value })} />
            <NumberField label="만기(년)" value={bondForm.maturityYears} onChange={(value) => setBondForm({ ...bondForm, maturityYears: value })} />
          </div>
          <button className="primary-button" type="submit" disabled={loading}>채권 평가 실행</button>
        </form>

        <form className="panel panel-project" onSubmit={(event) => {
          event.preventDefault();
          evaluate(
            "project",
            {
              ...projectForm,
              initialInvestment: toNumber(projectForm.initialInvestment),
              discountRate: Number(projectForm.discountRate),
              probabilityOfDefault: Number(projectForm.probabilityOfDefault),
              cashFlows: projectForm.cashFlows.map((value) => toNumber(value)),
            },
            setProjectResult,
            "project",
            "프로젝트 평가에 실패했습니다."
          );
        }}>
          <div className="panel-title">
            <h2>프로젝트 평가</h2>
            <span>현금흐름 기준</span>
          </div>
          <div className="field-grid">
            <label className="field">
              <span>프로젝트명</span>
              <input value={projectForm.projectName} onChange={(e) => setProjectForm({ ...projectForm, projectName: e.target.value })} />
            </label>
            <MoneyField label="초기 투자비" value={projectForm.initialInvestment} wide onChange={(value) => setProjectForm({ ...projectForm, initialInvestment: value })} onAdjust={(direction) => setProjectForm({ ...projectForm, initialInvestment: adjustByPercent(projectForm.initialInvestment, direction, 1) })} />
            <NumberField label="할인율" step="0.001" value={projectForm.discountRate} onChange={(value) => setProjectForm({ ...projectForm, discountRate: value })} />
            <NumberField label="부도확률" step="0.001" value={projectForm.probabilityOfDefault} onChange={(value) => setProjectForm({ ...projectForm, probabilityOfDefault: value })} />
            <CashFlowList cashFlows={projectForm.cashFlows} onChange={(cashFlows) => setProjectForm({ ...projectForm, cashFlows })} />
          </div>
          <button className="primary-button" type="submit" disabled={loading}>프로젝트 평가 실행</button>
        </form>
      </section>

      <section className="results-section">
        <ResultPanel
          title="주식 평가 결과"
          result={stockResult}
          inputs={stockForm}
          onSave={() => saveResult("stock", stockResult)}
          onToggleDetail={() => toggleDetail("stock")}
          detailOpen={detailOpen.stock}
          saving={savingState.stock}
          saved={savedState.stock}
        />
        <ResultPanel
          title="채권 평가 결과"
          result={bondResult}
          inputs={bondForm}
          onSave={() => saveResult("bond", bondResult)}
          onToggleDetail={() => toggleDetail("bond")}
          detailOpen={detailOpen.bond}
          saving={savingState.bond}
          saved={savedState.bond}
        />
        <ResultPanel
          title="프로젝트 평가 결과"
          result={projectResult}
          inputs={projectForm}
          onSave={() => saveResult("project", projectResult)}
          onToggleDetail={() => toggleDetail("project")}
          detailOpen={detailOpen.project}
          saving={savingState.project}
          saved={savedState.project}
        />
      </section>

      <RecentSection items={recentItems} />
    </div>
  );
}
