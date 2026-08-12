/* @ds-bundle: {"format":4,"namespace":"CookOffMaterial3_98ca42","components":[{"name":"Badge","sourcePath":"components/badge/Badge.jsx"},{"name":"Button","sourcePath":"components/button/Button.jsx"},{"name":"Card","sourcePath":"components/card/Card.jsx"},{"name":"Checkbox","sourcePath":"components/checkbox/Checkbox.jsx"},{"name":"Chip","sourcePath":"components/chip/Chip.jsx"},{"name":"Dialog","sourcePath":"components/dialog/Dialog.jsx"},{"name":"Divider","sourcePath":"components/divider/Divider.jsx"},{"name":"Fab","sourcePath":"components/fab/Fab.jsx"},{"name":"IconButton","sourcePath":"components/icon-button/IconButton.jsx"},{"name":"NavigationBar","sourcePath":"components/navigation-bar/NavigationBar.jsx"},{"name":"ProgressIndicator","sourcePath":"components/progress-indicator/ProgressIndicator.jsx"},{"name":"RadioButton","sourcePath":"components/radio-button/RadioButton.jsx"},{"name":"Snackbar","sourcePath":"components/snackbar/Snackbar.jsx"},{"name":"Switch","sourcePath":"components/switch/Switch.jsx"},{"name":"Tabs","sourcePath":"components/tabs/Tabs.jsx"},{"name":"TextField","sourcePath":"components/text-field/TextField.jsx"},{"name":"TopAppBar","sourcePath":"components/top-app-bar/TopAppBar.jsx"}],"sourceHashes":{"components/badge/Badge.jsx":"3b15b2158106","components/button/Button.jsx":"696ecea37887","components/card/Card.jsx":"608d1872d603","components/checkbox/Checkbox.jsx":"f4412f53010c","components/chip/Chip.jsx":"6a308ded84d8","components/dialog/Dialog.jsx":"1fbbf95b44b5","components/divider/Divider.jsx":"4d5c59ba487c","components/fab/Fab.jsx":"08b50f7da675","components/icon-button/IconButton.jsx":"20eb79cf3675","components/navigation-bar/NavigationBar.jsx":"4541733c3cba","components/progress-indicator/ProgressIndicator.jsx":"c5650a06ac54","components/radio-button/RadioButton.jsx":"fbe6dba4d17c","components/snackbar/Snackbar.jsx":"6445f424601a","components/switch/Switch.jsx":"1a7629578bf7","components/tabs/Tabs.jsx":"49888ea5e71e","components/text-field/TextField.jsx":"f13e5d60ba63","components/top-app-bar/TopAppBar.jsx":"f70e52be5d93","ui_kits/cookoff/Accounts.jsx":"54dd239fbd62","ui_kits/cookoff/ChallengeDetail.jsx":"31fd709ad5d5","ui_kits/cookoff/CookHome.jsx":"b1ee6ca089a6","ui_kits/cookoff/GuestHome.jsx":"0109f2b1b280","ui_kits/cookoff/History.jsx":"54fef06ef260","ui_kits/cookoff/Login.jsx":"a80a7c0f0856","ui_kits/cookoff/Register.jsx":"e96ad4ab96be","ui_kits/cookoff/Scoring.jsx":"6e177619c469"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.CookOffMaterial3_98ca42 = window.CookOffMaterial3_98ca42 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/badge/Badge.jsx
try { (() => {
function Badge({
  count,
  dot
}) {
  if (dot) return /*#__PURE__*/React.createElement("span", {
    className: "md-badge md-badge--dot"
  });
  return /*#__PURE__*/React.createElement("span", {
    className: "md-badge"
  }, count);
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/badge/Badge.jsx", error: String((e && e.message) || e) }); }

// components/button/Button.jsx
try { (() => {
function Button({
  variant = 'filled',
  label,
  icon,
  disabled,
  onClick,
  type = 'button'
}) {
  const cls = ['md-button', `md-button--${variant}`].join(' ');
  return /*#__PURE__*/React.createElement("button", {
    type: type,
    className: cls,
    disabled: disabled,
    onClick: onClick
  }, icon ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, icon) : null, label);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/button/Button.jsx", error: String((e && e.message) || e) }); }

// components/card/Card.jsx
try { (() => {
function Card({
  variant = 'elevated',
  media,
  title,
  subtitle,
  body,
  actions,
  clickable,
  onClick,
  children
}) {
  const cls = ['md-card', `md-card--${variant}`, clickable ? 'md-card--clickable' : ''].filter(Boolean).join(' ');
  return /*#__PURE__*/React.createElement("div", {
    className: cls,
    onClick: clickable ? onClick : undefined,
    role: clickable ? 'button' : undefined,
    tabIndex: clickable ? 0 : undefined
  }, media ? /*#__PURE__*/React.createElement("img", {
    className: "md-card__media",
    src: media,
    alt: ""
  }) : null, /*#__PURE__*/React.createElement("div", {
    className: "md-card__content"
  }, title ? /*#__PURE__*/React.createElement("div", {
    className: "md-card__title"
  }, title) : null, subtitle ? /*#__PURE__*/React.createElement("div", {
    className: "md-card__subtitle"
  }, subtitle) : null, body ? /*#__PURE__*/React.createElement("p", {
    className: "md-card__body"
  }, body) : null, children), actions ? /*#__PURE__*/React.createElement("div", {
    className: "md-card__actions"
  }, actions) : null);
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/card/Card.jsx", error: String((e && e.message) || e) }); }

// components/checkbox/Checkbox.jsx
try { (() => {
function Checkbox({
  label,
  checked,
  onChange,
  disabled
}) {
  return /*#__PURE__*/React.createElement("label", {
    className: "md-checkbox",
    style: disabled ? {
      opacity: 0.38,
      cursor: 'not-allowed'
    } : undefined
  }, /*#__PURE__*/React.createElement("input", {
    type: "checkbox",
    checked: checked,
    onChange: onChange,
    disabled: disabled
  }), /*#__PURE__*/React.createElement("span", {
    className: "md-checkbox__box"
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, "check")), label);
}
Object.assign(__ds_scope, { Checkbox });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/checkbox/Checkbox.jsx", error: String((e && e.message) || e) }); }

// components/chip/Chip.jsx
try { (() => {
function Chip({
  variant = 'assist',
  label,
  icon,
  selected,
  onClick,
  onRemove
}) {
  const cls = ['md-chip', variant === 'elevated' ? 'md-chip--elevated' : '', selected ? 'md-chip--selected' : ''].filter(Boolean).join(' ');
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: cls,
    onClick: onClick,
    "aria-pressed": variant === 'filter' ? !!selected : undefined
  }, variant === 'filter' ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined md-chip__check"
  }, "check") : null, icon && variant !== 'filter' ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, icon) : null, label, variant === 'input' ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined",
    onClick: e => {
      e.stopPropagation();
      onRemove && onRemove();
    }
  }, "close") : null);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/chip/Chip.jsx", error: String((e && e.message) || e) }); }

// components/dialog/Dialog.jsx
try { (() => {
function Dialog({
  icon,
  title,
  body,
  actions,
  onScrimClick
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "md-dialog-scrim",
    onClick: onScrimClick
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-dialog",
    role: "dialog",
    "aria-modal": "true",
    onClick: e => e.stopPropagation()
  }, icon ? /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined md-dialog__icon"
  }, icon) : null, /*#__PURE__*/React.createElement("div", {
    className: "md-dialog__title"
  }, title), /*#__PURE__*/React.createElement("div", {
    className: "md-dialog__body"
  }, body), /*#__PURE__*/React.createElement("div", {
    className: "md-dialog__actions"
  }, actions)));
}
Object.assign(__ds_scope, { Dialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/dialog/Dialog.jsx", error: String((e && e.message) || e) }); }

// components/divider/Divider.jsx
try { (() => {
function Divider({
  inset
}) {
  return /*#__PURE__*/React.createElement("hr", {
    className: inset ? 'md-divider md-divider--inset' : 'md-divider'
  });
}
Object.assign(__ds_scope, { Divider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/divider/Divider.jsx", error: String((e && e.message) || e) }); }

// components/fab/Fab.jsx
try { (() => {
function Fab({
  size = 'regular',
  variant = 'primary',
  icon,
  label,
  onClick
}) {
  const cls = ['md-fab', size !== 'regular' ? `md-fab--${size}` : '', variant === 'surface' ? 'md-fab--surface' : ''].filter(Boolean).join(' ');
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: cls,
    onClick: onClick
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, icon), label ? label : null);
}
Object.assign(__ds_scope, { Fab });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/fab/Fab.jsx", error: String((e && e.message) || e) }); }

// components/icon-button/IconButton.jsx
try { (() => {
function IconButton({
  variant = 'standard',
  icon,
  selected,
  disabled,
  onClick,
  label
}) {
  const cls = ['md-icon-button', variant !== 'standard' ? `md-icon-button--${variant}` : '', selected ? 'md-icon-button--selected' : ''].filter(Boolean).join(' ');
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: cls,
    disabled: disabled,
    onClick: onClick,
    "aria-label": label,
    "aria-pressed": selected
  }, /*#__PURE__*/React.createElement("span", {
    className: selected ? 'material-symbols-outlined filled' : 'material-symbols-outlined'
  }, icon));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/icon-button/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/navigation-bar/NavigationBar.jsx
try { (() => {
function NavigationBar({
  items,
  activeIndex,
  onChange
}) {
  return /*#__PURE__*/React.createElement("nav", {
    className: "md-nav-bar"
  }, items.map((it, i) => /*#__PURE__*/React.createElement("button", {
    key: it.label,
    type: "button",
    className: i === activeIndex ? 'md-nav-bar__item md-nav-bar__item--active' : 'md-nav-bar__item',
    onClick: () => onChange && onChange(i)
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, it.icon), it.label)));
}
Object.assign(__ds_scope, { NavigationBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation-bar/NavigationBar.jsx", error: String((e && e.message) || e) }); }

// components/progress-indicator/ProgressIndicator.jsx
try { (() => {
function ProgressIndicator({
  variant = 'linear',
  value
}) {
  if (variant === 'circular') return /*#__PURE__*/React.createElement("div", {
    className: "md-progress-circular",
    role: "progressbar"
  });
  return /*#__PURE__*/React.createElement("div", {
    className: "md-progress-linear",
    role: "progressbar",
    "aria-valuenow": value
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-progress-linear__bar",
    style: {
      transform: `scaleX(${(value ?? 60) / 100})`
    }
  }));
}
Object.assign(__ds_scope, { ProgressIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/progress-indicator/ProgressIndicator.jsx", error: String((e && e.message) || e) }); }

// components/radio-button/RadioButton.jsx
try { (() => {
function RadioButton({
  label,
  checked,
  onChange,
  name,
  disabled
}) {
  return /*#__PURE__*/React.createElement("label", {
    className: "md-radio",
    style: disabled ? {
      opacity: 0.38,
      cursor: 'not-allowed'
    } : undefined
  }, /*#__PURE__*/React.createElement("input", {
    type: "radio",
    name: name,
    checked: checked,
    onChange: onChange,
    disabled: disabled
  }), /*#__PURE__*/React.createElement("span", {
    className: "md-radio__dot"
  }), label);
}
Object.assign(__ds_scope, { RadioButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/radio-button/RadioButton.jsx", error: String((e && e.message) || e) }); }

// components/snackbar/Snackbar.jsx
try { (() => {
function Snackbar({
  message,
  actionLabel,
  onAction
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "md-snackbar"
  }, /*#__PURE__*/React.createElement("span", null, message), actionLabel ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "md-snackbar__action",
    onClick: onAction
  }, actionLabel) : null);
}
Object.assign(__ds_scope, { Snackbar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/snackbar/Snackbar.jsx", error: String((e && e.message) || e) }); }

// components/switch/Switch.jsx
try { (() => {
function Switch({
  checked,
  onChange,
  disabled,
  label
}) {
  return /*#__PURE__*/React.createElement("label", {
    className: "md-switch",
    style: disabled ? {
      opacity: 0.38,
      cursor: 'not-allowed'
    } : undefined,
    "aria-label": label
  }, /*#__PURE__*/React.createElement("input", {
    type: "checkbox",
    checked: checked,
    onChange: onChange,
    disabled: disabled
  }), /*#__PURE__*/React.createElement("span", {
    className: "md-switch__track"
  }), /*#__PURE__*/React.createElement("span", {
    className: "md-switch__handle"
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/switch/Switch.jsx", error: String((e && e.message) || e) }); }

// components/tabs/Tabs.jsx
try { (() => {
function Tabs({
  items,
  activeIndex,
  onChange
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "md-tabs"
  }, items.map((label, i) => /*#__PURE__*/React.createElement("button", {
    key: label,
    type: "button",
    className: i === activeIndex ? 'md-tab md-tab--active' : 'md-tab',
    onClick: () => onChange && onChange(i)
  }, label)));
}
Object.assign(__ds_scope, { Tabs });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/tabs/Tabs.jsx", error: String((e && e.message) || e) }); }

// components/text-field/TextField.jsx
try { (() => {
function TextField({
  variant = 'outlined',
  label,
  value,
  onChange,
  placeholder,
  type = 'text',
  error,
  supportingText,
  disabled
}) {
  const fieldCls = variant === 'outlined' ? 'md-text-field md-text-field--outlined' : 'md-text-field';
  return /*#__PURE__*/React.createElement("div", {
    className: error ? 'md-field md-field--error' : 'md-field'
  }, label ? /*#__PURE__*/React.createElement("label", {
    className: "md-field__label"
  }, label) : null, /*#__PURE__*/React.createElement("div", {
    className: "md-field__control"
  }, /*#__PURE__*/React.createElement("input", {
    className: fieldCls,
    type: type,
    value: value,
    onChange: onChange,
    placeholder: placeholder,
    disabled: disabled
  })), supportingText ? /*#__PURE__*/React.createElement("div", {
    className: "md-field__supporting"
  }, supportingText) : null);
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/text-field/TextField.jsx", error: String((e && e.message) || e) }); }

// components/top-app-bar/TopAppBar.jsx
try { (() => {
function TopAppBar({
  title,
  leadingIcon,
  onLeadingClick,
  actions,
  center
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: center ? 'md-top-app-bar md-top-app-bar--center' : 'md-top-app-bar'
  }, leadingIcon ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "md-icon-button",
    onClick: onLeadingClick,
    "aria-label": "Back"
  }, /*#__PURE__*/React.createElement("span", {
    className: "material-symbols-outlined"
  }, leadingIcon)) : null, /*#__PURE__*/React.createElement("div", {
    className: "md-top-app-bar__title"
  }, title), actions);
}
Object.assign(__ds_scope, { TopAppBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/top-app-bar/TopAppBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/Accounts.jsx
try { (() => {
const {
  TopAppBar,
  Chip,
  Button
} = window.CookOffMaterial3_98ca42;
function Accounts({
  accounts,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: "Accounts",
    leadingIcon: "arrow_back",
    onLeadingClick: onBack,
    actions: /*#__PURE__*/React.createElement(Button, {
      variant: "filled",
      label: "New",
      icon: "add"
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: '100%',
      borderCollapse: 'collapse'
    },
    className: "md-typescale-body-medium"
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    className: "md-typescale-label-small",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, /*#__PURE__*/React.createElement("th", {
    style: {
      textAlign: 'left',
      padding: 8,
      borderBottom: '2px solid var(--md-sys-color-outline-variant)'
    }
  }, "Name"), /*#__PURE__*/React.createElement("th", {
    style: {
      textAlign: 'left',
      padding: 8,
      borderBottom: '2px solid var(--md-sys-color-outline-variant)'
    }
  }, "Email"), /*#__PURE__*/React.createElement("th", {
    style: {
      textAlign: 'left',
      padding: 8,
      borderBottom: '2px solid var(--md-sys-color-outline-variant)'
    }
  }, "Roles"))), /*#__PURE__*/React.createElement("tbody", null, accounts.map(a => /*#__PURE__*/React.createElement("tr", {
    key: a.email
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      borderBottom: '1px solid var(--md-sys-color-outline-variant)'
    }
  }, a.name), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      borderBottom: '1px solid var(--md-sys-color-outline-variant)',
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, a.email), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      borderBottom: '1px solid var(--md-sys-color-outline-variant)'
    }
  }, a.roles.map(r => /*#__PURE__*/React.createElement(Chip, {
    key: r,
    variant: "suggestion",
    label: r
  })))))))));
}
window.Accounts = Accounts;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/Accounts.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/ChallengeDetail.jsx
try { (() => {
const {
  TopAppBar,
  Button,
  Chip
} = window.CookOffMaterial3_98ca42;
function ChallengeDetail({
  challenge,
  onBack,
  onReveal,
  onUnreveal
}) {
  if (!challenge) return null;
  const revealed = challenge.status === 'REVEALED';
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: challenge.dishName,
    leadingIcon: "arrow_back",
    onLeadingClick: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-label-medium",
    style: {
      color: 'var(--md-sys-color-primary)',
      textTransform: 'uppercase',
      letterSpacing: '.08em'
    }
  }, challenge.dateLabel), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 12,
      margin: '4px 0 4px'
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-headline-small"
  }, challenge.dishName), /*#__PURE__*/React.createElement(Chip, {
    variant: "filter",
    selected: revealed,
    label: revealed ? 'Revealed' : 'Open'
  })), /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-medium",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, challenge.title), /*#__PURE__*/React.createElement("hr", {
    className: "md-divider",
    style: {
      margin: '16px 0'
    }
  }), !revealed && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-title-small",
    style: {
      marginBottom: 8
    }
  }, "Guest list"), challenge.guests.map(g => /*#__PURE__*/React.createElement("div", {
    key: g.name,
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      padding: '10px 0',
      borderBottom: '1px solid var(--md-sys-color-outline-variant)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "md-typescale-body-medium"
  }, g.name), /*#__PURE__*/React.createElement(Chip, {
    variant: "filter",
    selected: g.submitted,
    label: g.submitted ? 'Submitted' : 'Pending'
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 12,
      marginTop: 20,
      flexWrap: 'wrap'
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outlined",
    label: "Send links",
    icon: "forward_to_inbox"
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "outlined",
    label: "Registration QR",
    icon: "qr_code_2"
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "filled",
    label: "Reveal results",
    onClick: onReveal
  }))), revealed && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("table", {
    style: {
      width: '100%',
      borderCollapse: 'collapse'
    },
    className: "md-typescale-body-medium"
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", null, /*#__PURE__*/React.createElement("th", null), /*#__PURE__*/React.createElement("th", {
    style: {
      padding: 8,
      background: 'color-mix(in srgb, #c0392b 13%, transparent)',
      textAlign: 'center'
    }
  }, challenge.cookA, challenge.winner === challenge.cookA ? ' 👑' : ''), /*#__PURE__*/React.createElement("th", {
    style: {
      padding: 8,
      background: 'color-mix(in srgb, #e0b400 13%, transparent)',
      textAlign: 'center'
    }
  }, challenge.cookB, challenge.winner === challenge.cookB ? ' 👑' : ''))), /*#__PURE__*/React.createElement("tbody", null, challenge.rows.map(r => /*#__PURE__*/React.createElement("tr", {
    key: r.label
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      borderBottom: '1px solid var(--md-sys-color-outline-variant)'
    }
  }, r.label), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      textAlign: 'center',
      borderBottom: '1px solid var(--md-sys-color-outline-variant)',
      fontWeight: r.a > r.b ? 600 : 400
    }
  }, r.a), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: 8,
      textAlign: 'center',
      borderBottom: '1px solid var(--md-sys-color-outline-variant)',
      fontWeight: r.b > r.a ? 600 : 400
    }
  }, r.b))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-small",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)',
      maxWidth: '40ch'
    }
  }, "Made a mistake? You can reopen scoring and hide the results again."), /*#__PURE__*/React.createElement(Button, {
    variant: "outlined",
    label: "Unreveal",
    icon: "undo",
    onClick: onUnreveal
  })))));
}
window.ChallengeDetail = ChallengeDetail;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/ChallengeDetail.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/CookHome.jsx
try { (() => {
const {
  TopAppBar,
  Card,
  RadioButton,
  Chip
} = window.CookOffMaterial3_98ca42;
function CookHome({
  name,
  open,
  past,
  onPickColor,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: `Hi, ${name}`,
    leadingIcon: "arrow_back",
    onLeadingClick: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-title-small",
    style: {
      marginBottom: 8
    }
  }, "Your open challenges"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      marginBottom: 24
    }
  }, open.map(c => /*#__PURE__*/React.createElement(Card, {
    key: c.id,
    variant: "outlined",
    title: c.dishName,
    subtitle: c.dateLabel
  }, c.color ? /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-body-small",
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 8,
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 12,
      height: 12,
      borderRadius: '50%',
      background: c.color === 'RED' ? '#c0392b' : '#e0b400'
    }
  }), "You're plating ", /*#__PURE__*/React.createElement("strong", null, c.color === 'RED' ? 'Red' : 'Yellow')) : /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(RadioButton, {
    name: `c-${c.id}`,
    label: "Red",
    onChange: () => onPickColor(c.id, 'RED')
  }), /*#__PURE__*/React.createElement(RadioButton, {
    name: `c-${c.id}`,
    label: "Yellow",
    onChange: () => onPickColor(c.id, 'YELLOW')
  })))), open.length === 0 && /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-medium",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, "Nothing open right now.")), /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-title-small",
    style: {
      marginBottom: 8
    }
  }, "Past challenges"), past.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      padding: '10px 0',
      borderBottom: '1px solid var(--md-sys-color-outline-variant)'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "md-typescale-body-medium"
  }, c.dishName, " \u2014 ", c.dateLabel), /*#__PURE__*/React.createElement(Chip, {
    variant: "filter",
    selected: true,
    label: "Revealed"
  })))));
}
window.CookHome = CookHome;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/CookHome.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/GuestHome.jsx
try { (() => {
const {
  TopAppBar,
  Card,
  Button,
  Chip
} = window.CookOffMaterial3_98ca42;
function GuestHome({
  name,
  open,
  past,
  onScore,
  onViewPast,
  onBack
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: `Hi, ${name}`,
    leadingIcon: "arrow_back",
    onLeadingClick: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-title-small",
    style: {
      marginBottom: 8
    }
  }, "Open scoring requests"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12,
      marginBottom: 24
    }
  }, open.map(c => /*#__PURE__*/React.createElement(Card, {
    key: c.id,
    variant: "filled",
    title: c.dishName,
    subtitle: c.dateLabel,
    actions: /*#__PURE__*/React.createElement(Button, {
      variant: c.submitted ? 'outlined' : 'filled',
      label: c.submitted ? 'Edit scores' : 'Score now',
      onClick: () => onScore(c.id)
    })
  })), open.length === 0 && /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-medium",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, "Nothing open right now.")), /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-title-small",
    style: {
      marginBottom: 8
    }
  }, "Past challenges"), past.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    onClick: () => onViewPast(c.id),
    style: {
      display: 'flex',
      justifyContent: 'space-between',
      padding: '10px 0',
      borderBottom: '1px solid var(--md-sys-color-outline-variant)',
      cursor: 'pointer'
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "md-typescale-body-medium"
  }, c.dishName, " \u2014 ", c.dateLabel), /*#__PURE__*/React.createElement(Chip, {
    variant: "filter",
    selected: true,
    label: "Revealed"
  })))));
}
window.GuestHome = GuestHome;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/GuestHome.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/History.jsx
try { (() => {
const {
  TopAppBar,
  IconButton,
  Button,
  Card,
  Chip
} = window.CookOffMaterial3_98ca42;
function History({
  challenges,
  onOpen,
  onCreate
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: "Challenges",
    actions: /*#__PURE__*/React.createElement(Button, {
      variant: "filled",
      label: "New",
      icon: "add",
      onClick: onCreate
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16,
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill,minmax(220px,1fr))',
      gap: 16
    }
  }, challenges.map(c => /*#__PURE__*/React.createElement(Card, {
    key: c.id,
    variant: "elevated",
    clickable: true,
    onClick: () => onOpen(c.id),
    title: c.dishName,
    subtitle: c.dateLabel,
    body: c.title,
    actions: /*#__PURE__*/React.createElement(Chip, {
      variant: c.status === 'OPEN' ? 'assist' : 'filter',
      label: c.status === 'OPEN' ? 'Open' : 'Revealed',
      selected: c.status !== 'OPEN'
    })
  }))));
}
window.History = History;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/History.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/Login.jsx
try { (() => {
const {
  useState
} = React;
const {
  Button,
  TextField
} = window.CookOffMaterial3_98ca42;
function Login({
  onLogin,
  onGuest,
  onCook,
  onRegister
}) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("form", {
    onSubmit: e => {
      e.preventDefault();
      onLogin();
    },
    style: {
      width: 320,
      display: 'flex',
      flexDirection: 'column',
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-label-medium",
    style: {
      color: 'var(--md-sys-color-primary)',
      textTransform: 'uppercase',
      letterSpacing: '.08em'
    }
  }, "CookOff"), /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-headline-small"
  }, "Organizer log in")), /*#__PURE__*/React.createElement(TextField, {
    label: "Email",
    value: email,
    onChange: e => setEmail(e.target.value),
    placeholder: "anna@example.com"
  }), /*#__PURE__*/React.createElement(TextField, {
    label: "Password",
    type: "password",
    value: password,
    onChange: e => setPassword(e.target.value),
    placeholder: "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "filled",
    type: "submit",
    label: "Log in"
  }), /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-small",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)',
      margin: 0
    }
  }, "Cooks and guests don't log in here \u2014 they get a personalized link by email for each challenge."), /*#__PURE__*/React.createElement("hr", {
    className: "md-divider"
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "text",
    label: "Preview a guest's link \u2192",
    onClick: onGuest
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "text",
    label: "Preview a cook's link \u2192",
    onClick: onCook
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "text",
    label: "Preview QR registration \u2192",
    onClick: onRegister
  })));
}
window.Login = Login;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/Login.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/Register.jsx
try { (() => {
const {
  useState
} = React;
const {
  TextField,
  Button
} = window.CookOffMaterial3_98ca42;
function Register({
  onBack
}) {
  const [done, setDone] = useState(false);
  const [first, setFirst] = useState('');
  if (done) {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        padding: 24
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 320,
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
        alignItems: 'center'
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        fontSize: 40
      }
    }, "\uD83C\uDF89"), /*#__PURE__*/React.createElement("div", {
      className: "md-typescale-title-large"
    }, "Welcome, ", first || 'friend', "!"), /*#__PURE__*/React.createElement("p", {
      className: "md-typescale-body-medium",
      style: {
        color: 'var(--md-sys-color-on-surface-variant)'
      }
    }, "Your account is set up as a participant. The host will send you a personalized link by email whenever you're part of a challenge.")));
  }
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100%',
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("form", {
    onSubmit: e => {
      e.preventDefault();
      setDone(true);
    },
    style: {
      width: 320,
      display: 'flex',
      flexDirection: 'column',
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-label-medium",
    style: {
      color: 'var(--md-sys-color-primary)',
      textTransform: 'uppercase',
      letterSpacing: '.08em'
    }
  }, "CookOff"), /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-headline-small"
  }, "Join the cook-off")), /*#__PURE__*/React.createElement(TextField, {
    label: "First name",
    value: first,
    onChange: e => setFirst(e.target.value),
    placeholder: "Sophie"
  }), /*#__PURE__*/React.createElement(TextField, {
    label: "Last name",
    placeholder: "Lang"
  }), /*#__PURE__*/React.createElement(TextField, {
    label: "Email",
    placeholder: "sophie@example.com"
  }), /*#__PURE__*/React.createElement(Button, {
    type: "submit",
    variant: "filled",
    label: "Register"
  })));
}
window.Register = Register;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/Register.jsx", error: String((e && e.message) || e) }); }

// ui_kits/cookoff/Scoring.jsx
try { (() => {
const {
  useState
} = React;
const {
  TopAppBar,
  Button
} = window.CookOffMaterial3_98ca42;
function Scoring({
  challenge,
  onBack,
  onSubmit
}) {
  const [answers, setAnswers] = useState({});
  const [hover, setHover] = useState({});
  const [submitted, setSubmitted] = useState(false);
  const categories = ['Mundgefühl', 'Tellersprache', 'Geschmack'];
  const set = (dish, cat, v) => setAnswers(s => ({
    ...s,
    [`${dish}_${cat}`]: v
  }));
  const allSet = categories.every(c => answers[`A_${c}`] && answers[`B_${c}`]);
  if (submitted) {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        gap: 16,
        textAlign: 'center',
        padding: 24
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        fontSize: 48
      }
    }, "\uD83C\uDF89"), /*#__PURE__*/React.createElement("div", {
      className: "md-typescale-headline-small"
    }, "Thanks \u2014 scores submitted!"), /*#__PURE__*/React.createElement("p", {
      className: "md-typescale-body-medium",
      style: {
        color: 'var(--md-sys-color-on-surface-variant)'
      }
    }, "Results reveal once the host closes scoring."), /*#__PURE__*/React.createElement(Button, {
      variant: "filled",
      label: "Back to home",
      onClick: () => onSubmit()
    }));
  }
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      height: '100%'
    }
  }, /*#__PURE__*/React.createElement(TopAppBar, {
    title: challenge.dishName,
    leadingIcon: "arrow_back",
    onLeadingClick: onBack
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto',
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("p", {
    className: "md-typescale-body-medium",
    style: {
      color: 'var(--md-sys-color-on-surface-variant)'
    }
  }, "Blind tasting \u2014 rate the red plate and the yellow plate, 1\u20135 stars each, without knowing who cooked which."), /*#__PURE__*/React.createElement("hr", {
    className: "md-divider",
    style: {
      margin: '12px 0 16px'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'grid',
      gridTemplateColumns: '1fr 2fr 2fr',
      gap: '12px 16px',
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement("div", null), /*#__PURE__*/React.createElement("div", {
    style: {
      background: '#c0392b',
      height: 8,
      borderRadius: 4
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      background: '#e0b400',
      height: 8,
      borderRadius: 4
    }
  }), categories.map(cat => /*#__PURE__*/React.createElement(React.Fragment, {
    key: cat
  }, /*#__PURE__*/React.createElement("div", {
    className: "md-typescale-body-medium"
  }, cat), /*#__PURE__*/React.createElement(Stars, {
    dish: "A",
    cat: cat,
    color: "#c0392b",
    value: hover[`A_${cat}`] ?? answers[`A_${cat}`] ?? 0,
    onSet: v => set('A', cat, v),
    onHover: v => setHover(h => ({
      ...h,
      [`A_${cat}`]: v
    })),
    onLeave: () => setHover(h => ({
      ...h,
      [`A_${cat}`]: undefined
    }))
  }), /*#__PURE__*/React.createElement(Stars, {
    dish: "B",
    cat: cat,
    color: "#e0b400",
    value: hover[`B_${cat}`] ?? answers[`B_${cat}`] ?? 0,
    onSet: v => set('B', cat, v),
    onHover: v => setHover(h => ({
      ...h,
      [`B_${cat}`]: v
    })),
    onLeave: () => setHover(h => ({
      ...h,
      [`B_${cat}`]: undefined
    }))
  })))), /*#__PURE__*/React.createElement(Button, {
    variant: "filled",
    label: "Submit scores",
    disabled: !allSet,
    onClick: () => setSubmitted(true)
  })));
}
function Stars({
  value,
  color,
  onSet,
  onHover,
  onLeave
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      justifyContent: 'center',
      gap: 2
    }
  }, [1, 2, 3, 4, 5].map(v => /*#__PURE__*/React.createElement("span", {
    key: v,
    onClick: () => onSet(v),
    onMouseEnter: () => onHover(v),
    onMouseLeave: onLeave,
    style: {
      fontSize: 26,
      cursor: 'pointer',
      color: v <= value ? color : 'var(--md-sys-color-outline-variant)'
    }
  }, "\u2605")));
}
window.Scoring = Scoring;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/cookoff/Scoring.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.Checkbox = __ds_scope.Checkbox;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.Dialog = __ds_scope.Dialog;

__ds_ns.Divider = __ds_scope.Divider;

__ds_ns.Fab = __ds_scope.Fab;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.NavigationBar = __ds_scope.NavigationBar;

__ds_ns.ProgressIndicator = __ds_scope.ProgressIndicator;

__ds_ns.RadioButton = __ds_scope.RadioButton;

__ds_ns.Snackbar = __ds_scope.Snackbar;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.Tabs = __ds_scope.Tabs;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.TopAppBar = __ds_scope.TopAppBar;

})();
